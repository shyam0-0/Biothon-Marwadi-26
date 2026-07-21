package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.LiveVitals
import com.medfusion.ai.domain.repository.LiveVitalsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Simulated ESP32 upload stream for Demo Mode — ticks a plausible reading
 * every few seconds so the Live Vitals card demonstrates real-time updates
 * with no backend or physical device required.
 */
@Singleton
class FakeLiveVitalsRepository @Inject constructor() : LiveVitalsRepository {

    override fun observeLatestVitals(patientId: String): Flow<Resource<LiveVitals?>> = flow {
        var heartRate = 76
        while (true) {
            heartRate = (heartRate + Random.nextInt(-3, 4)).coerceIn(64, 92)
            emit(
                Resource.Success(
                    LiveVitals(
                        heartRate = heartRate,
                        spo2 = Random.nextInt(96, 100),
                        deviceId = "esp32-demo",
                        timestamp = Instant.now().toString(),
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                )
            )
            delay(4_000)
        }
    }
}
