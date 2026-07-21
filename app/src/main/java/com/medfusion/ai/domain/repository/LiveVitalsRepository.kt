package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.LiveVitals
import kotlinx.coroutines.flow.Flow

/**
 * Live IoT vitals bridged through the FastAPI backend (Phase 7.3/7.4). One
 * document per patient, so this single repository serves both the patient's
 * own dashboard and the doctor's patient view — no duplicated read logic.
 */
interface LiveVitalsRepository {

    /**
     * Real-time latest reading for a patient: `Resource.Success(null)` means
     * no reading has arrived yet (distinct from a Firestore failure, which is
     * `Resource.Error`).
     */
    fun observeLatestVitals(patientId: String): Flow<Resource<LiveVitals?>>
}
