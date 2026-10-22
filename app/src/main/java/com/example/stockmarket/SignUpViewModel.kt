package com.example.stockmarket



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.AuthStore
import com.example.stockmarket.data.validateIdentifier
import com.example.stockmarket.data.validateName
import com.example.stockmarket.data.validatePassword
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val fullName: String = "",
    val identifier: String = "",
    val password: String = "",
    val confirm: String = "",
    val showPassword: Boolean = false,
    val showErrors: Boolean = false,   // errors appear only after the first tap on "Create account"
    val isLoading: Boolean = false,
    val formError: String? = null
) {
    // Each error is WORKED OUT from the current text, so it updates as you type
    val nameError: String? get() = if (showErrors) validateName(fullName) else null
    val identifierError: String? get() = if (showErrors) validateIdentifier(identifier) else null
    val passwordError: String? get() = if (showErrors) validatePassword(password) else null

    val confirmError: String?
        get() = if (showErrors && confirm != password) "Passwords do not match" else null
    val isValid: Boolean
        get() = validateName(fullName) == null &&
                validateIdentifier(identifier) == null &&
                validatePassword(password) == null &&
                confirm == password
}

class SignUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    // ---- Events from the screen. Typing clears the old form error. ----
    fun onNameChange(value: String) = _uiState.update { it.copy(fullName = value, formError = null) }
    fun onIdentifierChange(value: String) = _uiState.update { it.copy(identifier = value, formError = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, formError = null) }
    fun onConfirmChange(value: String) = _uiState.update { it.copy(confirm = value, formError = null) }
    fun onToggleShowPassword() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return

        // Not valid yet: start showing the errors, and stop here
        if (!state.isValid) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showErrors = true, formError = null) }
            delay(800) // pretend the server takes a moment to answer
            val error = AuthStore.register(state.fullName, state.identifier, state.password)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    formError = error,
                    // Do not keep the passwords in memory once they have been used
                    password = if (error == null) "" else it.password,
                    confirm = if (error == null) "" else it.confirm
                )
            }
            if (error == null) onSuccess()
        }
    }
}