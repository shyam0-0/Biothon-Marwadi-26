"""
Firebase Admin bootstrap. Loads the service account key from
firebase/serviceAccountKey.json if present. Missing credentials are handled
gracefully (logged, not raised) so the API can still start and serve
non-Firestore routes (e.g. /vitals in Phase 2) before the key is provisioned.
"""

import os

import firebase_admin
from firebase_admin import credentials, firestore

_SERVICE_ACCOUNT_PATH = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")

_app = None


def init_firebase():
    """Initializes the Firebase Admin app once. Returns None if credentials
    are missing or invalid instead of raising, so callers can degrade
    gracefully."""
    global _app
    if _app is not None:
        return _app

    if not os.path.isfile(_SERVICE_ACCOUNT_PATH):
        print(
            f"[firebase] No service account key found at {_SERVICE_ACCOUNT_PATH}. "
            "Firestore integration is disabled until it is provided."
        )
        return None

    try:
        cred = credentials.Certificate(_SERVICE_ACCOUNT_PATH)
        _app = firebase_admin.initialize_app(cred)
        print("[firebase] Firebase Admin initialized.")
        return _app
    except Exception as exc:  # noqa: BLE001 - never crash the API over this
        print(f"[firebase] Failed to initialize Firebase Admin: {exc}")
        return None


def get_firestore_client():
    """Returns a Firestore client, or None if Firebase isn't initialized."""
    app = init_firebase()
    if app is None:
        return None
    return firestore.client(app)
