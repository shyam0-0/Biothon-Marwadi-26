package com.medfusion.ai.ui.symptom

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.medfusion.ai.R
import com.medfusion.ai.domain.model.ReportAttachment
import com.medfusion.ai.domain.model.Severity
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.InlineErrorCard
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.SeverityChip
import com.medfusion.ai.ui.components.UploadPickerCard
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.AnalysisStage
import com.medfusion.ai.viewmodel.SymptomAnalysisViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * AI symptom analysis — the intelligent entry point. The patient describes their
 * symptoms, the AI returns possible conditions, severity and guidance, reports can
 * optionally be added to refine it, and only then does the patient decide whether
 * to consult a doctor. Appointments never open automatically.
 */
@Composable
fun SymptomAnalysisScreen(
    onConsult: (caseId: String, urgency: String, specialty: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SymptomAnalysisViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.consultEvents
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest { onConsult(it.caseId, it.urgency, it.specialty) }
    }

    val reportPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.addAttachment(uri.toAttachment(context))
    }

    MedFusionScaffold(title = "Symptom Check", onBack = onBack) { padding ->
        when (val stage = state.stage) {
            AnalysisStage.Analyzing ->
                LoadingView(
                    modifier = Modifier.padding(padding),
                    message = "Analysing your symptoms…",
                )

            is AnalysisStage.Failed ->
                Box(Modifier.fillMaxSize().padding(padding).padding(Sizes.screenPadding)) {
                    InlineErrorCard(error = stage.error, onRetry = viewModel::analyze)
                }

            AnalysisStage.Input ->
                InputContent(
                    symptoms = state.symptoms,
                    validationError = state.validationError,
                    onSymptomsChange = viewModel::onSymptomsChange,
                    onAnalyze = viewModel::analyze,
                    modifier = modifier.padding(padding),
                )

            is AnalysisStage.Result ->
                ResultContent(
                    analysis = stage.analysis,
                    attachments = state.attachments,
                    creatingCase = state.creatingCase,
                    consultError = state.consultError?.userMessage,
                    onAddReport = { reportPicker.launch("*/*") },
                    onRemoveReport = viewModel::removeAttachment,
                    onUpdateAnalysis = viewModel::analyze,
                    onConsult = viewModel::consult,
                    onNotNow = onBack,
                    modifier = modifier.padding(padding),
                )
        }
    }
}

@Composable
private fun InputContent(
    symptoms: String,
    validationError: String?,
    onSymptomsChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("How are you feeling?", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Describe your symptoms in your own words — when they started, how they feel, and anything " +
                "that makes them better or worse. Our AI assistant will help you understand what they might mean.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = symptoms,
            onValueChange = onSymptomsChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
            label = { Text("Your symptoms") },
            placeholder = { Text("e.g. Dry cough for 5 days with mild chest tightness and tiredness…") },
            shape = MaterialTheme.shapes.medium,
            isError = validationError != null,
            supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        )
        PrimaryButton(text = "Analyse my symptoms", onClick = onAnalyze)
        InfoBanner(text = stringResource(R.string.ai_disclaimer))
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun ResultContent(
    analysis: SymptomAnalysis,
    attachments: List<ReportAttachment>,
    creatingCase: Boolean,
    consultError: String?,
    onAddReport: () -> Unit,
    onRemoveReport: (ReportAttachment) -> Unit,
    onUpdateAnalysis: () -> Unit,
    onConsult: (specialty: String) -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (analysis.severity == Severity.EMERGENCY) {
            EmergencyBanner(analysis.emergencyMessage)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AI assessment", style = MaterialTheme.typography.headlineSmall)
            SeverityChip(severity = analysis.severity)
        }

        if (analysis.summary.isNotBlank()) {
            MedFusionCard(contentPadding = Spacing.lg) {
                Text("Summary", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                Text(analysis.summary, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (analysis.conditions.isNotEmpty()) {
            ConditionsCard(analysis)
        }

        InfoBanner(text = stringResource(R.string.ai_disclaimer))

        BulletCard("Recommended tests", analysis.recommendedTests, Icons.Outlined.Biotech)
        BulletCard("Recommended scans", analysis.recommendedScans, Icons.Outlined.Science)
        BulletCard("Home care", analysis.homeCare, Icons.Outlined.Home)
        BulletCard("Precautions", analysis.precautions, Icons.Outlined.Shield)
        BulletCard(
            "Red flags — seek care if these appear",
            analysis.redFlags,
            Icons.Outlined.Warning,
            tint = MaterialTheme.semantic.riskRed,
        )

        ReportsSection(
            recommendation = analysis.reportRecommendation,
            attachments = attachments,
            onAddReport = onAddReport,
            onRemoveReport = onRemoveReport,
            onUpdateAnalysis = onUpdateAnalysis,
        )

        ConsultationSection(
            analysis = analysis,
            creatingCase = creatingCase,
            consultError = consultError,
            onConsult = onConsult,
            onNotNow = onNotNow,
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun EmergencyBanner(message: String?) {
    val s = MaterialTheme.semantic
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = s.riskRed)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "Seek immediate medical care",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = s.riskRed,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message ?: "Your symptoms may indicate an emergency. Please contact emergency services " +
                "or go to the nearest hospital now.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ConditionsCard(analysis: SymptomAnalysis) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Possible conditions", style = MaterialTheme.typography.titleMedium)
        Text(
            "Estimated likelihoods — not a diagnosis.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        analysis.conditions.forEach { condition ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(condition.name, style = MaterialTheme.typography.bodyMedium)
                Text("${condition.confidence}%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(Spacing.xs))
            LinearProgressIndicator(
                progress = { (condition.confidence / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun BulletCard(
    title: String,
    items: List<String>,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    if (items.isEmpty()) return
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        items.forEach {
            Text("•  $it", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

@Composable
private fun ReportsSection(
    recommendation: String?,
    attachments: List<ReportAttachment>,
    onAddReport: () -> Unit,
    onRemoveReport: (ReportAttachment) -> Unit,
    onUpdateAnalysis: () -> Unit,
) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Add reports (optional)", style = MaterialTheme.typography.titleMedium)
        Text(
            recommendation ?: "If you have any lab reports, prescriptions, X-rays or scans, you can " +
                "add them to improve the analysis. This is entirely optional.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Spacer(Modifier.height(Spacing.md))
        attachments.forEach { attachment ->
            UploadPickerCard(
                title = "Report",
                hint = "",
                icon = Icons.Outlined.HealthAndSafety,
                selectedName = attachment.displayName,
                onPick = {},
                onClear = { onRemoveReport(attachment) },
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        SecondaryButton(text = "Add a report", onClick = onAddReport)
        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(text = "Update analysis with reports", onClick = onUpdateAnalysis)
        }
    }
}

@Composable
private fun ConsultationSection(
    analysis: SymptomAnalysis,
    creatingCase: Boolean,
    consultError: String?,
    onConsult: (specialty: String) -> Unit,
    onNotNow: () -> Unit,
) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MonitorHeart, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text("Next step", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (analysis.consultationRecommended)
                "Based on your symptoms, a consultation is recommended. Choose a specialist to book an appointment."
            else
                "A consultation may not be necessary, but you can still book one if you'd like.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))

        val specialists = analysis.recommendedSpecialists.ifEmpty { listOf("General Physician") }
        specialists.forEach { specialist ->
            PrimaryButton(
                text = "Consult $specialist",
                loading = creatingCase,
                onClick = { onConsult(specialist) },
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        consultError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(Spacing.sm))
        }
        SecondaryButton(text = "Not now — save my summary", onClick = onNotNow)
    }
}

/** Resolves a picked content Uri into a [ReportAttachment] (mime + display name). */
private fun Uri.toAttachment(context: Context): ReportAttachment {
    val mime = context.contentResolver.getType(this) ?: "application/octet-stream"
    var name: String? = null
    if (scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) name = cursor.getString(index)
        }
    }
    return ReportAttachment(uri = this, mimeType = mime, displayName = name ?: lastPathSegment ?: "Report")
}
