package com.example.stockmarket.data



import android.util.Patterns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

data class Account(
    val id: Int,
    val fullName: String,
    val identifier: String,
    val passwordHash: String
)

// ---------- Checks. Each returns an error message, or null when the value is fine ----------

fun validateName(name: String): String? =
    if (name.trim().length < 2) "Enter your full name" else null

fun validateIdentifier(text: String): String? {
    val value = text.trim()
    val isEmail = Patterns.EMAIL_ADDRESS.matcher(value).matches()
    val isPhone = value.length in 9..10 && value.all { it.isDigit() }
    return if (isEmail || isPhone) null else "Enter a valid email or phone number"
}

fun validatePassword(password: String): String? = when {
    password.length < 8 -> "Use at least 8 characters"
    password.none { it.isLetter() } || password.none { it.isDigit() } ->
        "Include at least one letter and one number"
    else -> null
}

// ---------- Hashing ----------

// SHA-256 turns any text into a long fixed string of letters and numbers.
// A real server uses a slower method made for passwords (bcrypt or Argon2) with a random "salt".
private fun hash(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

// ONE shared copy for the whole app. In the real app this lives on the server.
object AuthStore {

    // SAMPLE account so you can log in straight away: demo@mse.test / Password1
    private val accounts = mutableListOf(
        Account(1, "Demo User", "demo@mse.test", hash("Password1"))
    )
    private var nextId = 2

    private val _currentUser = MutableStateFlow<Account?>(null)
    val currentUser: StateFlow<Account?> = _currentUser.asStateFlow()

    // "Jane@Mail.com " and "jane@mail.com" are the same account
    private fun normalise(identifier: String) = identifier.trim().lowercase()

    // Returns an error message, or null when it worked
    fun register(fullName: String, identifier: String, password: String): String? {
        val id = normalise(identifier)
        if (accounts.any { it.identifier == id }) {
            return "An account with this email or number already exists"
        }
        val account = Account(nextId++, fullName.trim(), id, hash(password))
        accounts.add(account)
        _currentUser.value = account
        return null
    }

    // The message is the same whether the email or the password is wrong,
    // so nobody can use it to find out which emails have accounts.
    fun login(identifier: String, password: String): String? {
        val id = normalise(identifier)
        val account = accounts.find { it.identifier == id && it.passwordHash == hash(password) }
            ?: return "Wrong email, number or password"
        _currentUser.value = account
        return null
    }

    fun logout() {
        _currentUser.value = null
    }
}