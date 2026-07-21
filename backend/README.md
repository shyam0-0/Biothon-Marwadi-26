# MedFusion Backend (Phase 7.3)

A lightweight FastAPI bridge between IoT devices (starting with an ESP32) and
Firestore. It is completely separate from the Android app, which continues to
talk to Firestore directly for everything else.

```
ESP32 → HTTP POST /vitals → FastAPI Backend → Validation → Firestore → Android Patient/Doctor Portal
```

## 1. Python version

Python 3.10+ (tested with 3.10 and 3.13).

## 2. Create a virtual environment

From the repository root:

```bash
cd backend
python -m venv venv
```

Activate it:

- Windows (PowerShell): `venv\Scripts\Activate.ps1`
- Windows (cmd): `venv\Scripts\activate.bat`
- macOS/Linux: `source venv/bin/activate`

## 3. Install dependencies

```bash
pip install -r requirements.txt
```

## 4. Run the backend

Run from the **repository root** (one level above `backend/`), since the app
is structured as the `backend` package:

```bash
uvicorn backend.main:app --reload --host 0.0.0.0 --port 8000
```

Then check:

- `GET http://localhost:8000/` → `{"status": "MedFusion Backend Running"}`
- `GET http://localhost:8000/health` → Firestore connectivity, see below.

## 5. Firebase service account

Place your Firebase service account JSON key at:

```
backend/firebase/serviceAccountKey.json
```

This file is git-ignored. Generate it from Firebase Console →
Project Settings → Service Accounts → Generate new private key. If the file
is missing, the backend still starts and runs normally — Firestore writes are
simply skipped (logged, not raised) and `/health` reports `"disconnected"`
until the key is added (see `backend/firebase/firebase_service.py`).

## 6. How Firestore is used

Every accepted `POST /vitals` performs two writes via
`backend/services/firestore_service.py`:

| Purpose | Path | Behavior |
|---|---|---|
| History | `patients/{patientId}/vitals/{autoId}` | One new document per accepted reading (skipped for an exact duplicate resent within 5 seconds — see below) |
| Latest snapshot | `patients/{patientId}/latestVitals/current` | Always overwritten, so the Android app can observe the newest reading instantly without downloading history |

Both documents store the same fields:

- `deviceId`
- `heartRate`
- `spo2`
- `timestamp` (the device-supplied timestamp, stored as-is)
- `createdAt` (Firestore server timestamp)
- `source` — `"ESP32"`

**Validation** (`backend/models/vitals.py`): `heartRate` must be 30–220 BPM
and `spo2` 70–100%; anything outside that range — or a malformed payload —
is rejected with HTTP 400 and a descriptive `{"success": false, "message": ...}`
body instead of a stack trace.

**Duplicate protection**: an in-memory, process-local guard
(`is_duplicate_upload` in `firestore_service.py`) remembers the last accepted
reading per `(patientId, deviceId)`. If the same `heartRate` + `spo2` +
`timestamp` arrives again for that device within 5 seconds (e.g. a Wi-Fi
retry), the history write is skipped — the latest-vitals snapshot is still
refreshed and the response is still a success. This is intentionally simple:
no Redis, no persistence of the dedupe cache across restarts.

**Health check**: `GET /health` reports both backend liveness and whether
Firebase Admin is initialized — it never throws even if the service account
key is missing or invalid.

## 7. Example requests

### Success

```bash
curl -X POST http://localhost:8000/vitals \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "esp32_demo",
    "patientId": "demo_patient",
    "heartRate": 78,
    "spo2": 98,
    "timestamp": "2026-07-21T10:00:00Z"
  }'
```

```json
{
  "success": true,
  "message": "Vitals stored successfully."
}
```

### Failure (out-of-range value)

```bash
curl -X POST http://localhost:8000/vitals \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "esp32_demo",
    "patientId": "demo_patient",
    "heartRate": 400,
    "spo2": 98,
    "timestamp": "2026-07-21T10:00:00Z"
  }'
```

```json
{
  "success": false,
  "message": "Invalid heart rate."
}
```

### Health check

```bash
curl http://localhost:8000/health
```

```json
{
  "backend": "running",
  "firestore": "connected"
}
```

(`"firestore": "disconnected"` if no valid service account key is present.)

## 8. Folder structure

```
backend/
    main.py                    # FastAPI app, routes wiring, /health, /,
                                # and the validation-error -> 400 JSON handler
    requirements.txt
    routes/
        vitals.py               # POST /vitals: dedupe -> save history -> save latest -> respond
    services/
        firestore_service.py    # save_vitals, save_latest_vitals, is_duplicate_upload, is_firestore_connected
    models/
        vitals.py                # VitalsPayload (deviceId, patientId, heartRate, spo2, timestamp)
    firebase/
        firebase_service.py      # Firebase Admin init; serviceAccountKey.json goes here (git-ignored)
    README.md
```

## 9. Adding a future IoT device

`vitals.py` (route + model + service) is meant to be a template. A future
sensor — ECG, digital stethoscope, temperature, blood pressure, weight scale
— should add its own `models/<device>.py`, `services/<device>_service.py`,
and `routes/<device>.py` following the same shape (validate → dedupe → write
history + latest snapshot → consistent JSON response), then register its
router in `main.py`. None of those devices are implemented yet — this is only
the pattern to follow.
