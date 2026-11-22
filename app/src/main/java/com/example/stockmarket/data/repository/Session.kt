package com.example.stockmarket.data.repository

import android.content.Context
import com.example.stockmarket.data.local.SessionStorage
import com.example.stockmarket.data.local.StoredSession
import com.example.stockmarket.data.remote.Api
import com.example.stockmarket.data.remote.AuthResponse
import com.example.stockmarket.data.remote.ErrorResponse
import com.example.stockmarket.data.remote.LoginRequest
import com.example.stockmarket.data.remote.RegisterRequest
import com.example.stockmarket.data.remote.UserDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: UserDto) : SessionState
}


object SessionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    private var storage: SessionStorage? = null

    // Read by the network interceptor on other threads, so it is @Volatile
    @Volatile
    private var token: String? = null

    // A message for the Login screen, such as "Your session has expired"
    private var notice: String? = null

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun currentToken(): String? = token

    fun takeNotice(): String? {
        val message = notice
        notice = null
        return message
    }


    fun init(context: Context) {
        if (storage != null) return
        val sessionStorage = SessionStorage(context)
        storage = sessionStorage

        scope.launch {
            val saved = sessionStorage.read()
            if (saved == null) {
                _state.value = SessionState.LoggedOut
                TradingStore.clear()
                RegistrationStore.reset()
            } else {

                token = saved.token
                _state.value = SessionState.LoggedIn(saved.user)
                TradingStore.onLoggedIn()
                RegistrationStore.onLoggedIn()

                verify()
            }
        }
    }


    fun verify() {
        if (token == null) return
        scope.launch {
            try {
                val user = Api.auth.me()
                if (token != null) _state.value = SessionState.LoggedIn(user)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // ignored on purpose
            }
        }
    }

    suspend fun login(identifier: String, password: String): String? =
        authenticate { Api.auth.login(LoginRequest(identifier.trim(), password)) }

    suspend fun register(fullName: String, identifier: String, password: String): String? =
        authenticate { Api.auth.register(RegisterRequest(fullName.trim(), identifier.trim(), password)) }

    private suspend fun authenticate(request: suspend () -> AuthResponse): String? {
        return try {
            val response = request()
            token = response.token
            saveQuietly(StoredSession(response.token, response.user))
            _state.value = SessionState.LoggedIn(response.user)
            TradingStore.onLoggedIn()
            RegistrationStore.onLoggedIn()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            readServerMessage(e)
        } catch (e: UnknownHostException) {
            "No internet connection"
        } catch (e: IOException) {
            "Can't reach the server"
        } catch (e: Exception) {
            "Unexpected answer from the server"
        }
    }


    private suspend fun saveQuietly(session: StoredSession) {
        try {
            storage?.save(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // ignored on purpose
        }
    }


    private fun readServerMessage(e: HttpException): String {
        val text = try {
            e.response()?.errorBody()?.string()
        } catch (ex: Exception) {
            null
        }
        val message = try {
            gson.fromJson(text, ErrorResponse::class.java)?.error
        } catch (ex: Exception) {
            null
        }
        return message ?: "The server returned an error (${e.code()})"
    }

    fun logout() = endSession(null)

    fun onSessionExpired() = endSession("Your session has expired. Please log in again.")

    private fun endSession(message: String?) {
        if (_state.value is SessionState.LoggedOut) return   // already logged out

        token = null
        notice = message
        _state.value = SessionState.LoggedOut

        scope.launch {
            try {
                storage?.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

            }
        }
    }
}

