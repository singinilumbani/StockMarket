package com.example.stockmarket


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.stockmarket.feature.auth.SignUpContent
import com.example.stockmarket.feature.auth.SignUpUiState
import com.example.stockmarket.ui.theme.StockMarketTheme
import org.junit.Rule
import org.junit.Test

class SignUpContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: SignUpUiState) {
        composeRule.setContent {
            StockMarketTheme {
                SignUpContent(
                    state = state,
                    onBack = {},
                    onNameChange = {},
                    onIdentifierChange = {},
                    onPasswordChange = {},
                    onConfirmChange = {},
                    onToggleShowPassword = {},
                    onSubmit = {}
                )
            }
        }
    }

    @Test
    fun noErrors_areShown_beforeTheFirstSubmit() {
        show(
            SignUpUiState(
                fullName = "A",
                identifier = "abc",
                showErrors = false
            )
        )

        composeRule
            .onNodeWithText("Enter your full name")
            .assertDoesNotExist()

        composeRule
            .onNodeWithText("Enter a valid email or phone number")
            .assertDoesNotExist()
    }

    @Test
    fun badInput_showsEveryMessage_afterSubmit() {
        show(
            SignUpUiState(
                fullName = "A",
                identifier = "abc",
                password = "abcdefgh",
                confirm = "different",
                showErrors = true
            )
        )

        composeRule
            .onNodeWithText("Enter your full name")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Enter a valid email or phone number")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Include at least one letter and one number")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Passwords do not match")
            .assertIsDisplayed()
    }

    @Test
    fun formError_isShown() {
        show(
            SignUpUiState(
                formError = "An account with this email or number already exists"
            )
        )

        composeRule
            .onNodeWithText(
                "An account with this email or number already exists"
            )
            .assertIsDisplayed()
    }
}
