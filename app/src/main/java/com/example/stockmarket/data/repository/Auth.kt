package com.example.stockmarket.data.repository

// Checks for the sign-up form. They are for the user's convenience: the server
// repeats every check, because the server is the one that cannot be skipped.

private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

fun validateName(name: String): String? =
    if (name.trim().length < 2) "Enter your full name" else null

fun validateIdentifier(text: String): String? {
    val value = text.trim()
    val isEmail = EMAIL_REGEX.matches(value)
    val isPhone = value.length in 9..10 && value.all { it.isDigit() }
    return if (isEmail || isPhone) null else "Enter a valid email or phone number"
}

fun validatePassword(password: String): String? = when {
    password.length < 8 -> "Use at least 8 characters"
    password.none { it.isLetter() } || password.none { it.isDigit() } ->
        "Include at least one letter and one number"
    else -> null
}