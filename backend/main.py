from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from backend.routes import vitals
from backend.services.firestore_service import is_firestore_connected

app = FastAPI(title="MedFusion Backend")

app.include_router(vitals.router)

# Friendly, field-specific messages for the physiological range checks in
# models/vitals.py; anything else falls back to a generic "Invalid <field>."
_FIELD_MESSAGES = {
    "heartRate": "Invalid heart rate.",
    "spo2": "Invalid SpO2 value.",
}


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Converts Pydantic validation failures into the project's consistent
    {success, message} JSON on HTTP 400, instead of FastAPI's default 422
    error shape. Never raises — this is the last line of defense against a
    malformed IoT payload crashing the request."""
    first_error = exc.errors()[0]
    field = str(first_error.get("loc", ["payload"])[-1])
    message = _FIELD_MESSAGES.get(field, f"Invalid {field}.")
    return JSONResponse(status_code=400, content={"success": False, "message": message})


@app.get("/")
def root():
    return {"status": "MedFusion Backend Running"}


@app.get("/health")
def health():
    """Reports backend liveness plus Firestore connectivity, without ever
    throwing — a missing/invalid service account key just reports
    "disconnected" instead of failing the health check."""
    return {
        "backend": "running",
        "firestore": "connected" if is_firestore_connected() else "disconnected",
    }
