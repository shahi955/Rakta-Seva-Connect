package com.raktaseva.connect.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Wraps Google Play Services [FusedLocationProviderClient] for one-shot current location.
 * Falls back to [lastLocation] when high-accuracy current fix is unavailable.
 */
class LocationHelper(
    context: Context,
    private val fused: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
) {

    private val appContext = context.applicationContext

    /**
     * Returns null if runtime permissions are not granted or location cannot be resolved.
     */
    suspend fun getCurrentLocationOrNull(): Location? = withContext(Dispatchers.Main) {
        if (!hasLocationPermission(appContext)) return@withContext null

        val cts = CancellationTokenSource()
        val current = try {
            runCatching {
                fused.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()
            }.getOrNull()
        } finally {
            cts.cancel()
        }

        if (current != null && (current.latitude != 0.0 || current.longitude != 0.0)) {
            return@withContext current
        }

        runCatching { fused.lastLocation.await() }.getOrNull()
    }

    fun isLocationEnabled(): Boolean {
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    companion object {
        fun hasLocationPermission(context: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }
    }
}
