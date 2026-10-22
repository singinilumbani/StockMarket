package com.example.stockmarket

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.ui.theme.StockMarketTheme
import com.example.stockmarket.ui.theme.components.DetailHeader

// 1) STATEFUL: gets the ViewModel
@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    viewModel: SignUpViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    SignUpContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onToggleShowPassword = viewModel::onToggleShowPassword,
        onSubmit = { viewModel.submit(onSignedUp) }
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun SignUpContent(
    state: SignUpUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        DetailHeader(title = "Create account", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create your account to browse the market and, once registered, to trade.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AuthTextField(
                label = "Full name",
                value = state.fullName,
                onValueChange = onNameChange,
                error = state.nameError
            )

            AuthTextField(
                label = "Email or phone number",
                value = state.identifier,
                onValueChange = onIdentifierChange,
                error = state.identifierError,
                keyboardType = KeyboardType.Email
            )

            PasswordField(
                label = "Password",
                value = state.password,
                onValueChange = onPasswordChange,
                showPassword = state.showPassword,
                onToggleShow = onToggleShowPassword,
                error = state.passwordError,
                hint = "At least 8 characters, with a letter and a number"
            )

            PasswordField(
                label = "Confirm password",
                value = state.confirm,
                onValueChange = onConfirmChange,
                showPassword = state.showPassword,
                onToggleShow = onToggleShowPassword,
                error = state.confirmError,
                imeAction = ImeAction.Done,
                onDone = onSubmit
            )

            if (state.formError != null) {
                Text(
                    text = state.formError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "This is a university project. Use sample details only, and never a password you use anywhere else.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Create account")
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun SignUpContentPreview() {
    StockMarketTheme {
        Surface {
            SignUpContent(
                state = SignUpUiState(
                    fullName = "Test User",
                    identifier = "not-an-email",
                    password = "short",
                    confirm = "different",
                    showErrors = true
                ),
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

