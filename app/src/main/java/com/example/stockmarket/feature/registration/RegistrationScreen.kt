@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stockmarket.feature.registration

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stockmarket.data.repository.RegistrationDraft
import com.example.stockmarket.ui.components.DetailHeader
import com.example.stockmarket.ui.theme.StockMarketTheme

// "A function that takes an edit instruction" - keeps the signatures short
typealias DraftEdit = ((RegistrationDraft) -> RegistrationDraft) -> Unit

// 1) STATEFUL: gets the ViewModel
@Composable
fun RegistrationScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: RegistrationViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    RegistrationContent(
        draft = state.draft,
        onChange = viewModel::update,
        onNext = viewModel::next,
        onPrevious = viewModel::back,
        onSubmit = { viewModel.submit(onSubmitted) },
        onExit = { viewModel.saveAndExit(onBack) },
        submitError = state.error,
        isSubmitting = state.isSubmitting
    )
}

// 2) STATELESS: only draws what it is given
@Composable
fun RegistrationContent(
    draft: RegistrationDraft,
    onChange: DraftEdit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
    submitError: String? = null,
    isSubmitting: Boolean = false
) {
    val isLastStep = draft.step == registrationSteps.lastIndex
    val allowed = canContinue(draft) && !isSubmitting

    // The phone's Back button goes to the previous step first
    BackHandler(enabled = draft.step > 0) { onPrevious() }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {

        DetailHeader(
            title = "Register to trade",
            onBack = { if (draft.step > 0) onPrevious() else onExit() }
        )

        // Progress
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Step ${draft.step + 1} of ${registrationSteps.size}: ${registrationSteps[draft.step]}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (draft.step + 1) / registrationSteps.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // The current step (takes all the space that is left)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (draft.step) {
                0 -> PersonalStep(draft, onChange)
                1 -> IdStep(draft, onChange)
                2 -> SelfieStep(draft, onChange)
                3 -> PaymentStep(draft, onChange)
                else -> ReviewStep(draft, onChange)
            }
        }

        // Bottom area: messages, Back / Next, then Save and finish later
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // The server's reason when it refused the registration
            if (submitError != null) {
                Text(
                    text = submitError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (!allowed && !isSubmitting) {
                Text(
                    text = "Complete this step to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (draft.step > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Back") }
                }
                Button(
                    onClick = if (isLastStep) onSubmit else onNext,
                    enabled = allowed,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(if (isLastStep) "Submit" else "Next")
                    }
                }
            }

            TextButton(
                onClick = onExit,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save and finish later") }
        }
    }
}

// ---------- The five steps ----------

@Composable
private fun PersonalStep(d: RegistrationDraft, onChange: DraftEdit) {
    Hint("Use your details exactly as they appear on your ID. While testing, use sample details only.")
    FormField("Full name", d.fullName, { v -> onChange { it.copy(fullName = v) } })
    FormField(
        "Date of birth (DD/MM/YYYY)",
        d.dateOfBirth,
        { v -> onChange { it.copy(dateOfBirth = v) } })
    FormField("Home address", d.address, { v -> onChange { it.copy(address = v) } })
}

@Composable
private fun IdStep(d: RegistrationDraft, onChange: DraftEdit) {
    Text("ID type", fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("National ID", "Passport", "Licence").forEach { type ->
            FilterChip(
                selected = d.idType == type,
                onClick = { onChange { it.copy(idType = type) } },
                label = { Text(type) }
            )
        }
    }
    FormField("ID number", d.idNumber, { v -> onChange { it.copy(idNumber = v) } })
    PhotoCard(
        title = "Photo of your ID",
        taken = d.idPhotoTaken,
        onTake = { onChange { it.copy(idPhotoTaken = !it.idPhotoTaken) } }
    )
}

@Composable
private fun SelfieStep(d: RegistrationDraft, onChange: DraftEdit) {
    Hint("Take a clear photo of your face in good light. We compare it with your ID.")
    PhotoCard(
        title = "Selfie",
        taken = d.selfieTaken,
        onTake = { onChange { it.copy(selfieTaken = !it.selfieTaken) } }
    )
}

@Composable
private fun PaymentStep(d: RegistrationDraft, onChange: DraftEdit) {
    Text("How do you want to pay and receive money?", fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Mobile money", "Bank").forEach { method ->
            FilterChip(
                selected = d.payoutMethod == method,
                onClick = { onChange { it.copy(payoutMethod = method) } },
                label = { Text(method) }
            )
        }
    }
    FormField(
        label = if (d.payoutMethod == "Bank") "Bank account number" else "Mobile money number",
        value = d.accountNumber,
        onValueChange = { v -> onChange { it.copy(accountNumber = v) } },
        keyboardType = KeyboardType.Number
    )
}

@Composable
private fun ReviewStep(d: RegistrationDraft, onChange: DraftEdit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryRow("Name", d.fullName)
            SummaryRow("Date of birth", d.dateOfBirth)
            SummaryRow("Address", d.address)
            SummaryRow(d.idType, mask(d.idNumber))
            SummaryRow("ID photo", if (d.idPhotoTaken) "Added" else "Missing")
            SummaryRow("Selfie", if (d.selfieTaken) "Added" else "Missing")
            SummaryRow(d.payoutMethod, mask(d.accountNumber))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = d.riskAccepted,
            onCheckedChange = { checked -> onChange { it.copy(riskAccepted = checked) } }
        )
        Text(
            text = "I understand that investing in shares involves risk, including the loss of money.",
            modifier = Modifier.weight(1f)
        )
    }
}

// ---------- Small reusable pieces ----------

@Composable
private fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PhotoCard(title: String, taken: Boolean, onTake: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (taken) "Photo added ✓" else "No photo yet",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onTake) {
                Text(if (taken) "Retake photo" else "Take photo (simulated)")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

// Hides all but the last 2 characters: 12345678 -> ••••••78
private fun mask(text: String): String =
    if (text.length <= 2) text else "•".repeat(text.length - 2) + text.takeLast(2)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview
@Composable
fun RegistrationContentPreview() {
    StockMarketTheme {
        Surface {
            RegistrationContent(
                draft = RegistrationDraft(
                    step = 4,
                    fullName = "Test User",
                    dateOfBirth = "01/01/1990",
                    address = "Sample Road",
                    idNumber = "12345678",
                    idPhotoTaken = true,
                    selfieTaken = true,
                    accountNumber = "0999000000"
                ),
                onChange = {},
                onNext = {},
                onPrevious = {},
                onSubmit = {},
                onExit = {}
            )
        }
    }
}