package com.medfusion.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medfusion.ai.BuildConfig
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.fail
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.EmergencyEvents
import com.medfusion.ai.data.firebase.FirestoreSchema.Users
import com.medfusion.ai.data.remote.MedFusionApi
import com.medfusion.ai.data.remote.dto.EmergencyAlertRequest
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.EmergencyAction
import com.medfusion.ai.domain.model.EmergencyOutcome
import com.medfusion.ai.domain.model.Hospital
import com.medfusion.ai.domain.repository.EmergencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: MedFusionApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : EmergencyRepository {

    override suspend fun reportAnomaly(
        heartRate: Int,
        action: EmergencyAction,
        location: Pair<Double, Double>?,
    ): Resource<EmergencyOutcome> = withContext(io) {
        resourceOf {
            val uid = auth.currentUser?.uid ?: fail(AppError.Unauthorized())
            val emergencyContact = firestore.collection(Users.COLLECTION).document(uid)
                .get().await().getString(Users.EMERGENCY_CONTACT)

            val outcome = if (action == EmergencyAction.DISMISSED) {
                EmergencyOutcome(hospital = null, contactNotified = false)
            } else {
                escalate(uid, heartRate, emergencyContact, location)
            }

            // Audit trail: log every anomaly event regardless of the decision.
            val event = buildMap<String, Any?> {
                put(EmergencyEvents.PATIENT_ID, uid)
                put(EmergencyEvents.HEART_RATE, heartRate)
                put(EmergencyEvents.ACTION, action.wireValue)
                put(EmergencyEvents.HOSPITAL, outcome.hospital?.name)
                put(EmergencyEvents.LATITUDE, location?.first)
                put(EmergencyEvents.LONGITUDE, location?.second)
                put(EmergencyEvents.CREATED_AT, FieldValue.serverTimestamp())
            }
            firestore.collection(EmergencyEvents.COLLECTION).add(event).await()

            outcome
        }
    }

    private suspend fun escalate(
        uid: String,
        heartRate: Int,
        emergencyContact: String?,
        location: Pair<Double, Double>?,
    ): EmergencyOutcome = try {
        val response = api.emergencyAlert(
            EmergencyAlertRequest(
                patientId = uid,
                heartRate = heartRate,
                emergencyContact = emergencyContact,
                latitude = location?.first,
                longitude = location?.second,
            )
        )
        EmergencyOutcome(
            hospital = response.hospitalName?.let { Hospital(it, response.hospitalPhone.orEmpty()) },
            contactNotified = response.contactNotified,
        )
    } catch (t: Throwable) {
        if (BuildConfig.USE_MOCK_AI_FALLBACK) {
            // Demo escalation: pick the nearest hospital from a static list and
            // assume the SMS to the stored contact was dispatched.
            EmergencyOutcome(
                hospital = NEAREST_HOSPITALS.first(),
                contactNotified = emergencyContact != null,
            )
        } else throw t
    }

    private companion object {
        val NEAREST_HOSPITALS = listOf(
            Hospital("City General Hospital", "+1-202-555-0143", distanceKm = 2.1),
            Hospital("St. Mary's Medical Center", "+1-202-555-0182", distanceKm = 3.8),
            Hospital("Riverside Emergency Clinic", "+1-202-555-0111", distanceKm = 5.2),
        )
    }
}
