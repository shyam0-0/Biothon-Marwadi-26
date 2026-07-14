package com.medfusion.ai.ui.triage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.medfusion.ai.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.InlineErrorCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.TriageViewModel

/**
 * Symptom-only triage entry (no upload here). The patient describes symptoms; on
 * success a color-coded recommendation appears with a clear next step: get the
 * test done, then upload results.
 */
@Composable
fun SymptomTriageScreen(
    onUploadNow: (caseId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TriageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var symptoms by rememberSaveable { mutableStateOf("") }

    val isLoading = state is UiState.Loading
    val validationError = (state as? UiState.Error)?.error as? AppError.Validation
    val nonValidationError = (state as? UiState.Error)?.error?.takeIf { it !is AppError.Validation }
    val successCase = (state as? UiState.Success)?.data

    MedFusionScaffold(title = "Symptom Check", onBack = onBack) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                "Describe your symptoms",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Tell us what you're experiencing — when it started, how it feels, and anything that makes it better or worse.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = symptoms,
                onValueChange = {
                    symptoms = it
                    if (state is UiState.Error) viewModel.reset()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                label = { Text("Your symptoms") },
                placeholder = { Text("e.g. Persistent dry cough for 5 days with mild chest tightness…") },
                shape = MaterialTheme.shapes.medium,
                isError = validationError != null,
                supportingText = validationError?.let { { Text(it.userMessage, color = MaterialTheme.colorScheme.error) } },
                enabled = !isLoading && successCase == null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )

            if (successCase == null) {
                PrimaryButton(
                    text = "Get Recommendation",
                    loading = isLoading,
                    onClick = { viewModel.submit(symptoms) },
                )
            }

            nonValidationError?.let { error ->
                InlineErrorCard(error = error, onRetry = { viewModel.submit(symptoms) })
            }

            successCase?.let { case ->
                TriageResultCard(case = case, onUploadNow = { onUploadNow(case.caseId) })
                Spacer(Modifier.height(Spacing.sm))
                InfoBanner(text = stringResource(R.string.ai_disclaimer))
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
