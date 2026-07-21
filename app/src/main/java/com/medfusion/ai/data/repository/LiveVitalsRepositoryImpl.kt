package com.medfusion.ai.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.toAppError
import com.medfusion.ai.data.firebase.FirestoreSchema.LatestVitals
import com.medfusion.ai.domain.model.LiveVitals
import com.medfusion.ai.domain.repository.LiveVitalsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LiveVitals"

/**
 * Reads patients/{patientId}/latestVitals/current — the same document the
 * FastAPI backend writes (Phase 7.3). The Android app only ever observes this
 * path; it never writes to it.
 *
 * Phase 7.5: temporary Log.d calls trace the pipeline (path subscribed, raw
 * snapshot received, mapped Resource emitted) — remove once the live binding
 * is confirmed working end-to-end.
 */
@Singleton
class LiveVitalsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : LiveVitalsRepository {

    override fun observeLatestVitals(patientId: String): Flow<Resource<LiveVitals?>> = callbackFlow {
        val path = "${LatestVitals.PATIENTS_COLLECTION}/$patientId/${LatestVitals.SUBCOLLECTION}/${LatestVitals.DOCUMENT_ID}"
        Log.d(TAG, "[Repository] Subscribing to Firestore path: $path")

        val registration = firestore.collection(LatestVitals.PATIENTS_COLLECTION)
            .document(patientId)
            .collection(LatestVitals.SUBCOLLECTION)
            .document(LatestVitals.DOCUMENT_ID)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "[Firestore] Snapshot error on $path", error)
                    trySend(Resource.Error(error.toAppError()))
                    return@addSnapshotListener
                }

                Log.d(
                    TAG,
                    "[Firestore] Snapshot received for $path — exists=${snapshot?.exists()} " +
                        "data=${snapshot?.data}",
                )

                val mapped = snapshot?.toLiveVitals()
                Log.d(TAG, "[Repository] Emitting Resource.Success(data=$mapped)")
                trySend(Resource.Success(mapped))
            }
        awaitClose {
            Log.d(TAG, "[Repository] Removing listener on $path")
            registration.remove()
        }
    }
}

private fun DocumentSnapshot.toLiveVitals(): LiveVitals? {
    if (!exists()) return null
    val heartRate = getLong(LatestVitals.HEART_RATE)?.toInt()
    val spo2 = getLong(LatestVitals.SPO2)?.toInt()
    if (heartRate == null || spo2 == null) {
        Log.w(
            TAG,
            "[Repository] Document exists but heartRate/spo2 missing or wrong type — " +
                "raw fields=$data",
        )
        return null
    }
    return LiveVitals(
        heartRate = heartRate,
        spo2 = spo2,
        deviceId = getString(LatestVitals.DEVICE_ID).orEmpty(),
        timestamp = getString(LatestVitals.TIMESTAMP).orEmpty(),
        updatedAtMillis = getTimestamp(LatestVitals.CREATED_AT)?.toDate()?.time,
    )
}
