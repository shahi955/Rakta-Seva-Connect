package com.raktaseva.connect.util

import com.raktaseva.connect.model.firestore.UserDocument
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Haversine distance on WGS84 sphere. */
object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Keeps only users with valid coordinates within [radiusKm] of the center point.
     */
    fun filterWithinRadiusKm(
        users: List<UserDocument>,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double
    ): List<UserDocument> = users.filter { user ->
        val lat = user.latitude ?: return@filter false
        val lng = user.longitude ?: return@filter false
        distanceKm(centerLat, centerLng, lat, lng) <= radiusKm
    }

    fun withDistanceKm(
        users: List<UserDocument>,
        centerLat: Double,
        centerLng: Double
    ): List<DonorDistance> = users.mapNotNull { user ->
        val lat = user.latitude ?: return@mapNotNull null
        val lng = user.longitude ?: return@mapNotNull null
        DonorDistance(user, distanceKm(centerLat, centerLng, lat, lng))
    }.sortedBy { it.distanceKm }
}

data class DonorDistance(
    val user: UserDocument,
    val distanceKm: Double
)

/**
 * Named entry point for distance math (delegates to [GeoUtils] / Haversine).
 */
object DistanceCalculator {
    fun kilometersBetween(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double = GeoUtils.distanceKm(lat1, lon1, lat2, lon2)
}
