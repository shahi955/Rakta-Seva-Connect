package com.raktaseva.connect.util

import com.google.firebase.Timestamp
import com.raktaseva.connect.model.firestore.UserDocument

/**
 * Post-query filters for donor discovery (Firestore has no native radius / cooldown query).
 */
object DonorFilter {

    private const val MS_PER_DAY = 24L * 60 * 60 * 1000

    /**
     * Returns `true` if the donor is allowed to donate again (no donation in the last [cooldownDays] days).
     */
    fun isEligibleAfterLastDonation(
        lastDonationDate: Timestamp?,
        nowMs: Long = System.currentTimeMillis(),
        cooldownDays: Long = 90L
    ): Boolean {
        if (lastDonationDate == null) return true
        val lastMs = lastDonationDate.toDate().time
        val cooldownMs = cooldownDays * MS_PER_DAY
        return nowMs - lastMs >= cooldownMs
    }

    /**
     * Hides donors who donated within the last [cooldownDays] days (default 90).
     */
    fun excludeDonorsInCooldown(
        users: List<UserDocument>,
        nowMs: Long = System.currentTimeMillis(),
        cooldownDays: Long = 90L
    ): List<UserDocument> = users.filter { user ->
        isEligibleAfterLastDonation(user.lastDonationDate, nowMs, cooldownDays)
    }

    /**
     * Combines cooldown + radius using [GeoUtils.filterWithinRadiusKm].
     */
    fun filterEligibleNearbyDonors(
        users: List<UserDocument>,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double = 10.0,
        nowMs: Long = System.currentTimeMillis(),
        cooldownDays: Long = 90L
    ): List<DonorDistance> {
        val cooled = excludeDonorsInCooldown(users, nowMs, cooldownDays)
        val nearby = GeoUtils.filterWithinRadiusKm(cooled, centerLat, centerLng, radiusKm)
        return GeoUtils.withDistanceKm(nearby, centerLat, centerLng)
    }
}
