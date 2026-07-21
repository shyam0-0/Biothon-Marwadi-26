from pydantic import BaseModel, Field


class VitalsPayload(BaseModel):
    deviceId: str
    patientId: str
    heartRate: int = Field(ge=30, le=220, description="Beats per minute — physiologically valid range.")
    spo2: int = Field(ge=70, le=100, description="Blood oxygen saturation percentage.")
    timestamp: str
