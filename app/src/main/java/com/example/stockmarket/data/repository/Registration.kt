package com.example.stockmarket.data.repository

import com.example.stockmarket.data.model.RegistrationStatus
import com.example.stockmarket.data.remote.Api
import com.example.stockmarket.data.remote.KycDraftDto
import com.example.stockmarket.data.remote.KycDto
import com.example.stockmarket.data.remote.errorMessageOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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

private fun RegistrationDraft.toDto() = KycDraftDto(
    step, fullName, dateOfBirth, address, idType, idNumber,
    idPhotoTaken, selfieTaken, payoutMethod, accountNumber, riskAccepted
)

private fun KycDraftDto.toDraft() = RegistrationDraft(
    step, fullName, dateOfBirth, address, idType, idNumber,
    idPhotoTaken, selfieTaken, payoutMethod, accountNumber, riskAccepted
)

// A COPY of the logged-in user's registration. The server decides the status.
object RegistrationStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _status = MutableStateFlow(RegistrationStatus.NOT_STARTED)
    val status: StateFlow<RegistrationStatus> = _status.asStateFlow()

    private val _draft = MutableStateFlow(RegistrationDraft())
    val draft: StateFlow<RegistrationDraft> = _draft.asStateFlow()

    private var pollJob: Job? = null
    private var draftLoaded = false     // has the form been loaded from the server since login?
    private var generation = 0          // goes up on every logout, so late answers are ignored

    // ---------- Typing in the wizard (only changes the phone's copy) ----------

    fun updateDraft(edit: (RegistrationDraft) -> RegistrationDraft) {
        _draft.update(edit)
        val current = _status.value
        if (current == RegistrationStatus.NOT_STARTED || current == RegistrationStatus.REJECTED) {
            _status.value = RegistrationStatus.IN_PROGRESS
        }
    }

    // ---------- Talking to the server ----------

    // Called right after a login
    fun onLoggedIn() {
        scope.launch { refresh() }
    }

    // Returns an error message, or null when it worked
    suspend fun refresh(): String? {
        val gen = generation
        return try {
            val kyc = Api.kyc.get()
            if (gen == generation) apply(kyc)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessageOf(e)
        }
    }

    // Saves the form. Only needed while the user is filling it in.
    suspend fun saveDraft(): String? {
        if (_status.value != RegistrationStatus.IN_PROGRESS) return null
        val gen = generation
        return try {
            val kyc = Api.kyc.saveDraft(_draft.value.toDto())
            if (gen == generation) _status.value = RegistrationStatus.valueOf(kyc.status)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessageOf(e)
        }
    }

    // Fire and forget: used when the wizard is left
    fun saveInBackground() {
        scope.launch { saveDraft() }
    }

    // Sends the registration for review. Returns an error message, or null when it was accepted.
    suspend fun submit(): String? {
        val gen = generation
        saveDraft()?.let { return it }
        return try {
            val kyc = Api.kyc.submit()
            if (gen == generation) {
                apply(kyc)
                NotificationStore.add(
                    NotificationType.ACCOUNT,
                    "Registration submitted",
                    "We are reviewing your details."
                )
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessageOf(e)
        }
    }

    // Called when the user logs out, so the next user never sees this data
    fun reset() {
        generation++
        pollJob?.cancel()
        draftLoaded = false
        _draft.value = RegistrationDraft()
        _status.value = RegistrationStatus.NOT_STARTED
    }

    // ---------- Internals ----------

    private fun apply(kyc: KycDto) {
        val previous = _status.value
        val server = RegistrationStatus.valueOf(kyc.status)

        // The user is typing and has not saved yet: the phone's copy is newer than the server's
        if (previous == RegistrationStatus.IN_PROGRESS &&
            (server == RegistrationStatus.NOT_STARTED || server == RegistrationStatus.REJECTED)
        ) return

        // Take the server's form on the first load, and whenever it can no longer be edited,
        // so a refresh never wipes what someone is typing
        if (!draftLoaded ||
            server == RegistrationStatus.PENDING ||
            server == RegistrationStatus.APPROVED ||
            (server == RegistrationStatus.REJECTED && previous != RegistrationStatus.REJECTED)
        ) {
            _draft.value = kyc.draft.toDraft()
            draftLoaded = true
        }
        _status.value = server

        // The review finished while we were waiting: tell the user
        if (previous == RegistrationStatus.PENDING && server == RegistrationStatus.APPROVED) {
            NotificationStore.add(
                NotificationType.ACCOUNT,
                "Registration approved",
                "You can now place buy and sell orders."
            )
        }
        if (previous == RegistrationStatus.PENDING && server == RegistrationStatus.REJECTED) {
            NotificationStore.add(
                NotificationType.ACCOUNT,
                "Registration needs changes",
                kyc.rejectionReason ?: "Please fix your details and submit again."
            )
        }

        if (server == RegistrationStatus.PENDING) pollWhilePending()
    }

    // A person may take a while, so ask again every 3 seconds until the answer arrives
    private fun pollWhilePending() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            var tries = 0
            while (tries < 200 && _status.value == RegistrationStatus.PENDING) {
                delay(3_000)
                refresh()
                tries++
            }
        }
    }
}