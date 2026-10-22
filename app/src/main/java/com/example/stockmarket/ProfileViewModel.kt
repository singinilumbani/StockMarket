package com.example.stockmarket



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.RegistrationStatus
import com.example.stockmarket.data.RegistrationStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val name: String = "",
    val status: RegistrationStatus = RegistrationStatus.NOT_STARTED
) {
    val displayName: String get() = name.ifBlank { "Guest" }

    val initial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"
}

class ProfileViewModel : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        RegistrationStore.status,
        RegistrationStore.draft
    ) { status, draft ->
        ProfileUiState(name = draft.fullName, status = status)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileUiState(
            name = RegistrationStore.draft.value.fullName,
            status = RegistrationStore.status.value
        )
    )
}