package com.raktaseva.connect.util

object EmergencyRequestValidators {

    fun patientName(value: String): String? = when {
        value.isBlank() -> "Enter patient name"
        value.trim().length < 2 -> "Patient name is too short"
        else -> null
    }

    fun bloodGroup(value: String): String? =
        if (value.isBlank()) "Select blood group" else null

    fun units(value: String): String? {
        val n = value.toLongOrNull()
        return when {
            value.isBlank() -> "Enter units needed"
            n == null -> "Enter a valid number"
            n < 1 -> "At least 1 unit"
            n > 50 -> "Enter a realistic unit count (max 50)"
            else -> null
        }
    }

    fun hospital(value: String): String? =
        if (value.isBlank()) "Enter hospital name" else null

    fun contactNumber(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return when {
            value.isBlank() -> "Enter contact number"
            digits.length < 10 -> "Enter a valid contact number"
            else -> null
        }
    }

    fun emergencyLevel(value: String): String? =
        if (value.isBlank()) "Select emergency level" else null

    fun notes(value: String): String? =
        if (value.length > 500) "Notes must be 500 characters or less" else null

    fun location(lat: Double?, lng: Double?): String? =
        if (lat == null || lng == null) "Set request location (use current location)" else null
}
