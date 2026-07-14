package com.medfusion.ai.ui.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.R
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.ui.components.ConfidenceChip
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.InlineErrorCard
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.StateContainer
import com.medfusion.ai.ui.components.UrgencyChip
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.DownloadUiState
import com.medfusion.ai.viewmodel.ResultViewModel

/**
 * Explainable result: plain-language findings, a confidence band, the mandatory
 * "not a diagnosis" disclaimer, and a clear path to book an appointment.
 */
@Composable
fun ResultScreen(
    onBookAppointment: (caseId: String, urgency: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Notifications back the download-complete alert; request on 33+ then download.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.downloadReport() }

    val startDownload: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.downloadReport()
        }
    }

    MedFusionScaffold(title = "Your Result", onBack = onBack) { padding ->
        StateContainer(
            state = state,
            modifier = Modifier.padding(padding),
            loadingMessage = "Loading your result…",
            onRetry = viewModel::load,
        ) { case ->
            ResultContent(
                case = case,
                downloadState = downloadState,
                onBookAppointment = { onBookAppointment(case.caseId, case.urgencyLevel.wireValue) },
                onDownload = startDownload,
                onDismissDownloadError = viewModel::consumeDownloadState,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ResultContent(
    case: Case,
    downloadState: DownloadUiState,
    onBookAppointment: () -> Unit,
    onDownload: () -> Unit,
    onDismissDownloadError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fusion = case.fusionResult

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AI-assisted insight", style = MaterialTheme.typography.headlineSmall)
            UrgencyChip(level = case.urgencyLevel)
        }

        MedFusionCard(contentPadding = Spacing.lg) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Findings",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                fusion?.findings
                    ?: "Your analysis is being prepared. Please check back shortly.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (fusion != null) {
                Spacer(Modifier.height(Spacing.md))
                ConfidenceChip(level = fusion.confidenceLevel)
            }
        }

        InfoBanner(text = stringResource(R.string.ai_disclaimer))

        Spacer(Modifier.height(Spacing.sm))
        PrimaryButton(
            text = "Book Appointment",
            leadingIcon = Icons.Outlined.CalendarMonth,
            onClick = onBookAppointment,
        )
        SecondaryButton(
            text = "Download Report",
            leadingIcon = Icons.Outlined.Download,
            enabled = downloadState !is DownloadUiState.Downloading,
            onClick = onDownload,
        )

        when (val d = downloadState) {
            is DownloadUiState.Downloading -> DownloadStatusText("Preparing your report…")
            is DownloadUiState.Done -> DownloadStatusText(d.message)
            is DownloadUiState.Error -> InlineErrorCard(error = d.error, onRetry = onDismissDownloadError)
            DownloadUiState.Idle -> Unit
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun DownloadStatusText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
