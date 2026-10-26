
package com.example.stockmarket


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.stockmarket.feature.auth.LoginContent
import com.example.stockmarket.feature.auth.LoginUiState
import com.example.stockmarket.ui.theme.StockMarketTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: LoginUiState,
        onCreateAccount: () -> Unit = {}
    ) {
        composeRule.setContent {
            StockMarketTheme {
                LoginContent(
                    state = state,
                    onIdentifierChange = {},
                    onPasswordChange = {},
                    onToggleShowPassword = {},
                    onLogin = {},
                    onCreateAccount = onCreateAccount
                )
            }
        }
    }

    @Test
    fun loginButton_isDisabled_whenFieldsAreEmpty() {
        show(LoginUiState())

        composeRule.onNodeWithText("Log in").assertIsNotEnabled()
    }

    @Test
    fun loginButton_isEnabled_whenBothFieldsAreFilled() {
        show(
            LoginUiState(
                identifier = "demo@mse.test",
                password = "Password1"
            )
        )

        composeRule.onNodeWithText("Log in").assertIsEnabled()
    }

    @Test
    fun errorMessage_isShown() {
        show(
            LoginUiState(
                error = "Wrong email, number or password"
            )
        )

        composeRule
            .onNodeWithText("Wrong email, number or password")
            .assertIsDisplayed()
    }

    @Test
    fun createAccount_callsTheCallback() {
        var clicked = false

        show(
            LoginUiState(),
            onCreateAccount = { clicked = true }
        )

        composeRule
            .onNodeWithText("New here? Create an account")
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun forgotPassword_opensTheDialog() {
        show(LoginUiState())

        composeRule
            .onNodeWithText("Forgot password?")
            .performClick()

        composeRule
            .onNodeWithText("Reset your password")
            .assertIsDisplayed()
    }

    @Test
    fun passwordField_hasAShowPasswordToggle() {
        show(LoginUiState())

        composeRule
            .onNodeWithContentDescription("Show password")
            .assertIsDisplayed()
    }
}


