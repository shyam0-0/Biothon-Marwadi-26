package com.medfusion.ai.data.remote

import com.medfusion.ai.data.remote.dto.AnalyzeRequest
import com.medfusion.ai.data.remote.dto.AnalyzeResponse
import com.medfusion.ai.data.remote.dto.CreateRoomRequest
import com.medfusion.ai.data.remote.dto.CreateRoomResponse
import com.medfusion.ai.data.remote.dto.EmergencyAlertRequest
import com.medfusion.ai.data.remote.dto.EmergencyAlertResponse
import com.medfusion.ai.data.remote.dto.GeneratePdfRequest
import com.medfusion.ai.data.remote.dto.GeneratePdfResponse
import com.medfusion.ai.data.remote.dto.TriageRequest
import com.medfusion.ai.data.remote.dto.TriageResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit contract for the MedFusion FastAPI backend. Endpoints are added as
 * their phases land (/analyze — Phase 4, /generate-pdf — Phase 6, /create-room —
 * Phase 9, SMS/hospital alerts — Phase 11).
 */
interface MedFusionApi {

    /** Symptom triage — returns a recommended test and an urgency level. */
    @POST("triage")
    suspend fun triage(@Body request: TriageRequest): TriageResponse

    /** Multimodal fusion analysis of symptoms + uploaded reports. */
    @POST("analyze")
    suspend fun analyze(@Body request: AnalyzeRequest): AnalyzeResponse

    /** Server-rendered PDF report — returns a downloadable Storage URL. */
    @POST("generate-pdf")
    suspend fun generatePdf(@Body request: GeneratePdfRequest): GeneratePdfResponse

    /** Creates a video-consultation room for an appointment; returns its URL. */
    @POST("create-room")
    suspend fun createRoom(@Body request: CreateRoomRequest): CreateRoomResponse

    /** Escalates a heart-rate emergency: SMS the contact + alert nearest hospital. */
    @POST("emergency-alert")
    suspend fun emergencyAlert(@Body request: EmergencyAlertRequest): EmergencyAlertResponse
}
