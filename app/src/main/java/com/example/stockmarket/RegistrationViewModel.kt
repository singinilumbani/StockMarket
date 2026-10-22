package com.example.stockmarket


import androidx.lifecycle.ViewModel
import com.example.stockmarket.data.RegistrationDraft
import com.example.stockmarket.data.RegistrationStore
import kotlinx.coroutines.flow.StateFlow

val registrationSteps = listOf(
    "Personal details",
    "ID document",
    "Selfie",
    "Payment details",
    "Review and submit"
)

// Decides whether the Next button is allowed on the current step
fun canContinue(d: RegistrationDraft): Boolean = when (d.step) {
    0 -> d.fullName.isNotBlank() && d.dateOfBirth.isNotBlank() && d.address.isNotBlank()
    1 -> d.idNumber.length >= 4 && d.idPhotoTaken
    2 -> d.selfieTaken
    3 -> d.accountNumber.isNotBlank()
    else -> d.riskAccepted
}

class RegistrationViewModel : ViewModel() {

    val draft: StateFlow<RegistrationDraft> = RegistrationStore.draft

    fun update(edit: (RegistrationDraft) -> RegistrationDraft) = RegistrationStore.updateDraft(edit)

    fun next() = update { it.copy(step = (it.step + 1).coerceAtMost(registrationSteps.lastIndex)) }

    fun back() = update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun submit() = RegistrationStore.submit()
}