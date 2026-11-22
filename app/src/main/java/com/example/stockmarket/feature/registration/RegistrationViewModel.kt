package com.example.stockmarket.feature.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockmarket.data.repository.RegistrationDraft
import com.example.stockmarket.data.repository.RegistrationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val registrationSteps = listOf(
    "Personal details",
    "ID document",
    "Selfie",
    "Payment details",
    "Review and submit"
)

// Decides whether the Next button is allowed on the current step.
// (The server checks everything again when you submit.)
fun canContinue(d: RegistrationDraft): Boolean = when (d.step) {
    0 -> d.fullName.isNotBlank() && d.dateOfBirth.isNotBlank() && d.address.isNotBlank()
    1 -> d.idNumber.length >= 4 && d.idPhotoTaken
    2 -> d.selfieTaken
    3 -> d.accountNumber.isNotBlank()
    else -> d.riskAccepted
}

data class RegistrationUiState(
    val draft: RegistrationDraft = RegistrationDraft(),
    val isSubmitting: Boolean = false,
    val error: String? = null          // the server's message when a submit is refused
)

class RegistrationViewModel : ViewModel() {

    private val isSubmitting = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RegistrationUiState> = combine(
        RegistrationStore.draft, isSubmitting, error
    ) { draft, submitting, problem ->
        RegistrationUiState(draft, submitting, problem)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = RegistrationUiState(draft = RegistrationStore.draft.value)
    )

    // ---- Events from the screen ----
    fun update(edit: (RegistrationDraft) -> RegistrationDraft) {
        error.value = null
        RegistrationStore.updateDraft(edit)
    }

    fun next() {
        update { it.copy(step = (it.step + 1).coerceAtMost(registrationSteps.lastIndex)) }
        RegistrationStore.saveInBackground()          // keep the server's copy up to date
    }

    fun back() {
        update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
        RegistrationStore.saveInBackground()
    }

    fun saveAndExit(onDone: () -> Unit) {
        RegistrationStore.saveInBackground()
        onDone()
    }

    fun submit(onSuccess: () -> Unit) {
        if (isSubmitting.value) return                // blocks a second tap at once
        isSubmitting.value = true
        error.value = null

        viewModelScope.launch {
            val problem = RegistrationStore.submit()    // null when the server accepted it
            isSubmitting.value = false
            error.value = problem
            if (problem == null) onSuccess()
        }
    }

    // Leaving the wizard any other way (for example the system Back button) also saves
    override fun onCleared() {
        RegistrationStore.saveInBackground()
    }
}