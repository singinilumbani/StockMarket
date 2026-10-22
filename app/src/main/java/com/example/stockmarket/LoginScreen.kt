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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.ui.theme.StockMarketTheme

// 1) STATEFUL: gets the ViewModel
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LoginContent(
        state = state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onToggleShowPassword = viewModel::onToggleShowPassword,
        onLogin = { viewModel.login(onLoggedIn) },
        onCreateAccount = onCreateAccount
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun LoginContent(
    state: LoginUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onToggleShowPassword: () -> Unit,
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit
) {
    var forgotOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MSE Invest",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Invest in Malawi's listed companies from your phone.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        AuthTextField(
            label = "Email or phone number",
            value = state.identifier,
            onValueChange = onIdentifierChange,
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(12.dp))

        PasswordField(
            label = "Password",
            value = state.password,
            onValueChange = onPasswordChange,
            showPassword = state.showPassword,
            onToggleShow = onToggleShowPassword,
            imeAction = ImeAction.Done,
            onDone = onLogin
        )

        if (state.error != null) {
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(
            onClick = { forgotOpen = true },
            modifier = Modifier.align(Alignment.End)
        ) { Text("Forgot password?") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onLogin,
            enabled = state.canLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Log in")
            }
        }

        TextButton(
            onClick = onCreateAccount,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("New here? Create an account") }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Sample account for testing: demo@mse.test / Password1",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (forgotOpen) {
        AlertDialog(
            onDismissRequest = { forgotOpen = false },
            title = { Text("Reset your password") },
            text = {
                Text("In the full app we send a reset link to your email or phone. This is simulated in the project.")
            },
            confirmButton = { TextButton(onClick = { forgotOpen = false }) { Text("OK") } }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun LoginContentPreview() {
    StockMarketTheme {
        Surface {
            LoginContent(
                state = LoginUiState(
                    identifier = "demo@mse.test",
                    password = "wrong",
                    error = "Wrong email, number or password"
                ),
                onIdentifierChange = {},
                onPasswordChange = {},
                onToggleShowPassword = {},
                onLogin = {},
                onCreateAccount = {}
            )
        }
    }
}