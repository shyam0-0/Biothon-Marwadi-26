package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.EmergencyAction
import com.medfusion.ai.domain.model.EmergencyOutcome

/** Emergency escalation + audit logging for heart-rate anomalies (Phase 11). */
interface EmergencyRepository {

    /**
     * Logs the anomaly event and, unless [action] is DISMISSED, escalates it:
     * SMS the emergency contact and alert the nearest hospital with the patient's
     * location (via backend, with a static hospital fallback in demo builds).
     */
    suspend fun reportAnomaly(
        heartRate: Int,
        action: EmergencyAction,
        location: Pair<Double, Double>?,
    ): Resource<EmergencyOutcome>
}
