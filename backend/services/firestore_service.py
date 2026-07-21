"""
Firestore writes for IoT vitals ingestion: history, a latest-value snapshot,
lightweight duplicate suppression, and a connectivity check for /health.

Kept generic enough (payload -> field dict -> two writes) that a future
sensor type (ECG, stethoscope, temperature, BP, weight) can add its own
Pydantic model + a sibling service module following the same shape, without
touching this one.
"""

import time

from firebase_admin import firestore

from backend.firebase.firebase_service import get_firestore_client
from backend.models.vitals import VitalsPayload

# Lightweight in-memory duplicate guard: remembers the last accepted reading
# per (patientId, deviceId) so an ESP32 retrying the same POST within a short
# window (e.g. a Wi-Fi retry) doesn't pile up duplicate history entries.
# Process-local by design — no Redis/DB needed for a single-instance backend.
_DUPLICATE_WINDOW_SECONDS = 5.0
_last_reading: dict[tuple[str, str], tuple[int, int, str, float]] = {}


def is_duplicate_upload(payload: VitalsPayload) -> bool:
    """True if this exact reading was just accepted for this device within
    the dedupe window. Also records this reading as the new "last seen" one."""
    key = (payload.patientId, payload.deviceId)
    now = time.monotonic()
    previous = _last_reading.get(key)
    duplicate = (
        previous is not None
        and previous[0] == payload.heartRate
        and previous[1] == payload.spo2
        and previous[2] == payload.timestamp
        and (now - previous[3]) < _DUPLICATE_WINDOW_SECONDS
    )
    _last_reading[key] = (payload.heartRate, payload.spo2, payload.timestamp, now)
    return duplicate


def is_firestore_connected() -> bool:
    """Cheap connectivity check for /health — true if Firebase Admin is
    initialized. Does not perform a network round trip."""
    return get_firestore_client() is not None


def save_vitals(payload: VitalsPayload):
    """Appends one vitals reading to patients/{patientId}/vitals/{autoId}.

    Returns the new document id, or None if Firestore isn't available
    (missing/invalid credentials) — callers must handle that gracefully
    rather than treating it as a crash.
    """
    db = get_firestore_client()
    if db is None:
        print("[firestore] Skipped save_vitals: Firestore is not initialized.")
        return None

    doc_ref = (
        db.collection("patients")
        .document(payload.patientId)
        .collection("vitals")
        .document()
    )
    doc_ref.set(_vitals_fields(payload))
    return doc_ref.id


def save_latest_vitals(payload: VitalsPayload):
    """Overwrites patients/{patientId}/latestVitals/current with this
    reading, so the Android app can later observe the newest values without
    downloading the full history.

    Returns True on success, False if Firestore isn't available.
    """
    db = get_firestore_client()
    if db is None:
        print("[firestore] Skipped save_latest_vitals: Firestore is not initialized.")
        return False

    doc_ref = (
        db.collection("patients")
        .document(payload.patientId)
        .collection("latestVitals")
        .document("current")
    )
    doc_ref.set(_vitals_fields(payload))
    return True


def _vitals_fields(payload: VitalsPayload) -> dict:
    """Shared field shape for both the history entry and the latest-vitals
    snapshot, so the two can never drift apart."""
    return {
        "deviceId": payload.deviceId,
        "heartRate": payload.heartRate,
        "spo2": payload.spo2,
        "timestamp": payload.timestamp,
        "createdAt": firestore.SERVER_TIMESTAMP,
        "source": "ESP32",
    }
