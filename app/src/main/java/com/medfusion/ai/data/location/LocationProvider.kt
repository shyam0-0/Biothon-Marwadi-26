package com.medfusion.ai.data.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Best-effort last-known location using the framework [LocationManager] (no
 * Play Services dependency). Returns null when permission is missing or no fix is
 * available, so callers degrade gracefully.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun lastKnown(): Pair<Double, Double>? {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return null

        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = manager.getProviders(true)
            val location = providers
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .maxByOrNull { it.time }
            location?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) {
            null
        }
    }
}
