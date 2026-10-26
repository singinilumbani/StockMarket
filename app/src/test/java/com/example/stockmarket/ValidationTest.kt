package com.example.stockmarket



import com.example.stockmarket.data.repository.validateIdentifier
import com.example.stockmarket.data.repository.validateName
import com.example.stockmarket.data.repository.validatePassword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// Tests for the sign-up rules. Traces to FR-01 and NFR-01.
class ValidationTest {

    // ---- Names ----
    @Test
    fun validateName_oneLetter_returnsError() {
        assertNotNull(validateName("A"))
    }

    @Test
    fun validateName_normalName_returnsNull() {
        assertNull(validateName("Test User"))
    }

    // ---- Passwords ----
    @Test
    fun validatePassword_tooShort_asksForEightCharacters() {
        assertEquals("Use at least 8 characters", validatePassword("abc1"))
    }

    @Test
    fun validatePassword_noDigit_asksForLetterAndNumber() {
        assertEquals("Include at least one letter and one number", validatePassword("abcdefgh"))
    }

    @Test
    fun validatePassword_validPassword_returnsNull() {
        assertNull(validatePassword("Sample123"))
    }

    // ---- Email or phone number ----
    @Test
    fun validateIdentifier_validEmail_returnsNull() {
        assertNull(validateIdentifier("demo@mse.test"))
    }

    @Test
    fun validateIdentifier_validPhone_returnsNull() {
        assertNull(validateIdentifier("0999123456"))
    }

    @Test
    fun validateIdentifier_notAnEmailOrPhone_returnsError() {
        assertEquals("Enter a valid email or phone number", validateIdentifier("abc"))
        assertEquals("Enter a valid email or phone number", validateIdentifier("a@b"))
        assertEquals("Enter a valid email or phone number", validateIdentifier("12345"))
    }
}