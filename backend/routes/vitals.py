from fastapi import APIRouter
from fastapi.responses import JSONResponse

from backend.models.vitals import VitalsPayload
from backend.services.firestore_service import (
    is_duplicate_upload,
    save_latest_vitals,
    save_vitals,
)

router = APIRouter()


@router.post("/vitals")
def receive_vitals(payload: VitalsPayload):
    """Validated ESP32 upload: appends to history (deduped within a short
    window) and overwrites the latest-vitals snapshot, then returns the
    project's standard {success, message} JSON."""
    duplicate = is_duplicate_upload(payload)

    try:
        if not duplicate:
            save_vitals(payload)
        save_latest_vitals(payload)
    except Exception as exc:  # noqa: BLE001 - never crash the API over a write failure
        _log_failure(payload, exc)
        return JSONResponse(
            status_code=503,
            content={"success": False, "message": "Failed to store vitals. Please retry."},
        )

    _log_success(payload, duplicate)
    return {"success": True, "message": "Vitals stored successfully."}


def _log_success(payload: VitalsPayload, duplicate: bool) -> None:
    print("-----------------------------------")
    print("ESP32 Upload Received")
    print()
    print(f"Patient:\n{payload.patientId}")
    print()
    print(f"Device:\n{payload.deviceId}")
    print()
    print(f"Heart Rate:\n{payload.heartRate} BPM")
    print()
    print(f"SpO2:\n{payload.spo2} %")
    print()
    print("Duplicate resend - history skipped, latest updated" if duplicate else "Stored Successfully")
    print("-----------------------------------")


def _log_failure(payload: VitalsPayload, exc: Exception) -> None:
    print("-----------------------------------")
    print("ESP32 Upload FAILED")
    print()
    print(f"Patient:\n{payload.patientId}")
    print(f"Device:\n{payload.deviceId}")
    print(f"Error:\n{exc}")
    print("-----------------------------------")
