package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.EmergencyAction
import com.medfusion.ai.domain.model.EmergencyOutcome
import com.medfusion.ai.domain.model.Hospital
import com.medfusion.ai.domain.repository.EmergencyRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/** Simulated emergency escalation for Demo Mode. */
@Singleton
class FakeEmergencyRepository @Inject constructor() : EmergencyRepository {
    override suspend fun reportAnomaly(
        heartRate: Int,
        action: EmergencyAction,
        location: Pair<Double, Double>?,
    ): Resource<EmergencyOutcome> {
        delay(800)
        return if (action == EmergencyAction.DISMISSED) {
            Resource.Success(EmergencyOutcome(hospital = null, contactNotified = false))
        } else {
            Resource.Success(
                EmergencyOutcome(
                    hospital = Hospital("City General Hospital", "+1-202-555-0143", distanceKm = 2.1),
                    contactNotified = true,
                )
            )
        }
    }
}
