package com.example.stockmarket



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.AuthStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canLogin: Boolean
        get() = identifier.isNotBlank() && password.isNotBlank() && !isLoading
}

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // ---- Events from the screen. Typing clears the old error message. ----
    fun onIdentifierChange(value: String) = _uiState.update { it.copy(identifier = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }
    fun onToggleShowPassword() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.canLogin) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(700) // pretend the server takes a moment to answer
            val error = AuthStore.login(state.identifier, state.password)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = error,
                    // Do not keep the password in memory once it has been used
                    password = if (error == null) "" else it.password
                )
            }
            if (error == null) onSuccess()
        }
    }
}