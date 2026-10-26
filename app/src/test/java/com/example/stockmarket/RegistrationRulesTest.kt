package com.example.stockmarket


import com.example.stockmarket.data.repository.RegistrationDraft
import com.example.stockmarket.feature.registration.canContinue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests for when the wizard's Next button is allowed. Traces to FR-01.
class RegistrationRulesTest {

    @Test
    fun step1_emptyDraft_cannotContinue() {
        assertFalse(canContinue(RegistrationDraft(step = 0)))
    }

    @Test
    fun step1_allFieldsFilled_canContinue() {
        val draft = RegistrationDraft(
            step = 0, fullName = "Test User", dateOfBirth = "01/01/1990", address = "Sample Road"
        )
        assertTrue(canContinue(draft))
    }

    @Test
    fun step1_missingAddress_cannotContinue() {
        val draft = RegistrationDraft(step = 0, fullName = "Test User", dateOfBirth = "01/01/1990")
        assertFalse(canContinue(draft))
    }

    @Test
    fun step2_shortIdNumber_cannotContinue() {
        assertFalse(canContinue(RegistrationDraft(step = 1, idNumber = "123", idPhotoTaken = true)))
    }

    @Test
    fun step2_noIdPhoto_cannotContinue() {
        assertFalse(canContinue(RegistrationDraft(step = 1, idNumber = "12345678", idPhotoTaken = false)))
    }

    @Test
    fun step2_idNumberAndPhoto_canContinue() {
        assertTrue(canContinue(RegistrationDraft(step = 1, idNumber = "12345678", idPhotoTaken = true)))
    }

    @Test
    fun step3_selfieRequired() {
        assertFalse(canContinue(RegistrationDraft(step = 2, selfieTaken = false)))
        assertTrue(canContinue(RegistrationDraft(step = 2, selfieTaken = true)))
    }

    @Test
    fun step4_accountNumberRequired() {
        assertFalse(canContinue(RegistrationDraft(step = 3, accountNumber = "")))
        assertTrue(canContinue(RegistrationDraft(step = 3, accountNumber = "0999000000")))
    }

    @Test
    fun step5_riskMustBeAccepted() {
        assertFalse(canContinue(RegistrationDraft(step = 4, riskAccepted = false)))
        assertTrue(canContinue(RegistrationDraft(step = 4, riskAccepted = true)))
    }
}