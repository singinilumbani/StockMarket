package com.example.stockmarket.data



import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrationDraft(
    val step: Int = 0,
    val fullName: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val idType: String = "National ID",
    val idNumber: String = "",
    val idPhotoTaken: Boolean = false,
    val selfieTaken: Boolean = false,
    val payoutMethod: String = "Mobile money",
    val accountNumber: String = "",
    val riskAccepted: Boolean = false
)

// ONE shared copy for the whole app. In the real app this data lives on the server.
object RegistrationStore {

    // Its own scope, so the "review" keeps running after the wizard screen closes
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow(RegistrationStatus.NOT_STARTED)
    val status: StateFlow<RegistrationStatus> = _status.asStateFlow()

    private val _draft = MutableStateFlow(RegistrationDraft())
    val draft: StateFlow<RegistrationDraft> = _draft.asStateFlow()

    private val _rejectionReason = MutableStateFlow("")
    val rejectionReason: StateFlow<String> =
        _rejectionReason.asStateFlow()

    fun updateDraft(edit: (RegistrationDraft) -> RegistrationDraft) {
        _draft.update(edit)
        val current = _status.value
        if (current == RegistrationStatus.NOT_STARTED || current == RegistrationStatus.REJECTED) {
            _status.value = RegistrationStatus.IN_PROGRESS
        }
    }


    // Simulates the broker reviewing the application
    fun submit() {
        val idNumber = _draft.value.idNumber
        _status.value = RegistrationStatus.PENDING
        NotificationStore.add(
            NotificationType.ACCOUNT,
            "Registration submitted",
            "We are reviewing your details."
        )
        scope.launch {
            delay(5_000)
            if (idNumber == "0000") {
                // Test trick: an ID number of 0000 is always rejected
                _draft.update { it.copy(step = 0, riskAccepted = false) }
                _status.value = RegistrationStatus.REJECTED
                NotificationStore.add(
                    NotificationType.ACCOUNT,
                    "Registration needs changes",
                    "Please fix your details and submit again."
                )
            } else {
                _status.value = RegistrationStatus.APPROVED
                NotificationStore.add(
                    NotificationType.ACCOUNT,
                    "Registration approved",
                    "You can now place buy and sell orders."
                )
            }
        }
    }
}