package com.medfusion.ai.ui.upload

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.InlineErrorCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.UploadPickerCard
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.UploadState
import com.medfusion.ai.viewmodel.UploadViewModel

/**
 * Upload screen for a specific case. The patient attaches an X-ray/scan and/or a
 * lab report (PDF or image); on success the case advances to analysis automatically.
 */
@Composable
fun UploadResultsScreen(
    onAnalysisReady: (caseId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var xrayUri by remember { mutableStateOf<Uri?>(null) }
    var labUri by remember { mutableStateOf<Uri?>(null) }

    val xrayPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) xrayUri = uri }

    val labPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) labUri = uri }

    val inProgress = state is UploadState.InProgress
    val success = state as? UploadState.Success

    // Once the case is ready, hand off to the analysis step automatically.
    if (success != null) {
        androidx.compose.runtime.LaunchedEffect(success.case.caseId) {
            onAnalysisReady(success.case.caseId)
        }
    }

    MedFusionScaffold(title = "Upload Results", onBack = onBack) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Upload your test results", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Add your X-ray/scan image and/or your lab report. Our AI will analyse them together with your symptoms.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            UploadPickerCard(
                title = "X-ray / Scan",
                hint = "Tap to select an image",
                icon = Icons.Outlined.Image,
                selectedName = xrayUri?.displayName(context),
                onPick = { xrayPicker.launch("image/*") },
                onClear = { xrayUri = null },
                enabled = !inProgress,
            )

            UploadPickerCard(
                title = "Lab Report",
                hint = "Tap to select a PDF or image",
                icon = Icons.Outlined.Description,
                selectedName = labUri?.displayName(context),
                onPick = { labPicker.launch("*/*") },
                onClear = { labUri = null },
                enabled = !inProgress,
            )

            when (val s = state) {
                is UploadState.InProgress -> UploadProgressSection(s.fraction, s.message)
                is UploadState.Error -> InlineErrorCard(
                    error = s.error,
                    onRetry = { viewModel.retryAfterError() },
                )
                else -> Unit
            }

            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(
                text = "Upload & Analyse",
                enabled = (xrayUri != null || labUri != null) && success == null,
                loading = inProgress,
                onClick = { viewModel.upload(xrayUri, labUri) },
            )

            InfoBanner(
                text = "Your files are stored securely and used only for your care.",
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun UploadProgressSection(fraction: Float, message: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

/** Best-effort human-readable file name from a content Uri. */
private fun Uri.displayName(context: android.content.Context): String {
    var name: String? = null
    if (scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
    }
    return name ?: lastPathSegment ?: "Selected file"
}
