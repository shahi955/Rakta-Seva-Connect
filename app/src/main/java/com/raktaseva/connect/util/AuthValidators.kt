package com.raktaseva.connect.util

import java.util.regex.Pattern

object AuthValidators {

    private val EMAIL = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
        Pattern.CASE_INSENSITIVE
    )

    fun validateName(value: String): String? = when {
        value.isBlank() -> "Enter your name"
        value.trim().length < 2 -> "Name is too short"
        else -> null
    }

    fun validateEmail(value: String): String? = when {
        value.isBlank() -> "Enter your email"
        !EMAIL.matcher(value.trim()).matches() -> "Enter a valid email"
        else -> null
    }

    fun validatePassword(value: String): String? = when {
        value.length < 8 -> "Use at least 8 characters"
        !value.any { it.isDigit() } -> "Include at least one number"
        else -> null
    }

    fun validatePasswordForSignIn(value: String): String? =
        if (value.isBlank()) "Enter your password" else null

    fun validatePhone(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return when {
            value.isBlank() -> "Enter your phone number"
            digits.length < 10 -> "Enter a valid phone number"
            else -> null
        }
    }

    fun validateCity(value: String): String? =
        if (value.isBlank()) "Enter your city" else null

    fun validateTaluk(value: String): String? =
        if (value.isBlank()) "Enter your taluk" else null

    fun validateBloodGroup(value: String): String? =
        if (value.isBlank()) "Select your blood group" else null
}
