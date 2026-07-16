package com.medfusion.ai.ui.consultation

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.R
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.PatientExplanation
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.StateContainer
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.PrescriptionViewModel

/** Patient-facing digital prescription from a completed consultation (Phase 2). */
@Composable
fun PrescriptionScreen(
    onBack: () -> Unit,
    onOpenCarePlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrescriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val explanation by viewModel.explanation.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Prescription", onBack = onBack) { padding ->
        StateContainer(
            state = state,
            modifier = Modifier.padding(padding),
            emptyTitle = "No prescription yet",
            emptySubtitle = "Your prescription will appear here after the doctor completes the consultation.",
            onRetry = viewModel::load,
        ) { prescription ->
            Content(prescription, explanation, viewModel::retryExplanation, onOpenCarePlan, modifier)
        }
    }
}

@Composable
private fun Content(
    prescription: Prescription,
    explanation: UiState<PatientExplanation>,
    onRetryExplanation: () -> Unit,
    onOpenCarePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MedFusionCard {
            Text("Diagnosis", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(prescription.diagnosis.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Prescribed by ${prescription.doctorName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (prescription.medications.isNotEmpty()) {
            Text("Medications", style = MaterialTheme.typography.titleMedium)
            prescription.medications.forEach { med ->
                MedFusionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Medication, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(med.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${med.dosage} • ${med.timing}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (prescription.advice.isNotBlank()) {
            MedFusionCard {
                Text("Advice", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(prescription.advice, style = MaterialTheme.typography.bodyMedium)
            }
        }

        prescription.followUpDate?.let {
            MedFusionCard {
                Text("Next follow-up", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Patient-friendly medical translation (Phase 5.6). Shown only to the
        // patient — the doctor's clinical record above is never modified.
        ExplanationCard(explanation, onRetryExplanation)

        // Phase 4: the prescription flows straight into recovery tracking.
        PrimaryButton(text = "View my care plan", onClick = onOpenCarePlan)

        InfoBanner(text = stringResource(R.string.ai_disclaimer))
        Spacer(Modifier.height(16.dp))
    }
}

/** "Your Doctor's Explanation" — the plain-language translation (Phase 5.6). */
@Composable
private fun ExplanationCard(state: UiState<PatientExplanation>, onRetry: () -> Unit) {
    MedFusionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.HealthAndSafety, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Your Doctor's Explanation",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(8.dp))
        when (state) {
            is UiState.Idle, is UiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Preparing an easy-to-understand summary…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is UiState.Error -> {
                Text("We couldn't prepare your explanation right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SecondaryButton(text = "Try again", onClick = onRetry)
            }
            is UiState.Empty -> Unit
            is UiState.Success -> ExplanationSections(state.data)
        }
    }
}

@Composable
private fun ExplanationSections(explanation: PatientExplanation) {
    ExpandableSection("What your doctor found", Icons.Outlined.MonitorHeart, initiallyExpanded = true) {
        Text(explanation.whatDoctorFound, style = MaterialTheme.typography.bodyMedium)
    }
    if (explanation.medicines.isNotEmpty()) {
        ExpandableSection("Why you received these medicines", Icons.Outlined.Medication) {
            explanation.medicines.forEach {
                Text(it.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 4.dp))
                Text(it.purpose, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (explanation.whatToDo.isNotEmpty()) {
        ExpandableSection("What you should do", Icons.AutoMirrored.Outlined.DirectionsRun) {
            explanation.whatToDo.forEach {
                Text("•  $it", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
    if (explanation.recovery.isNotBlank()) {
        ExpandableSection("Recovery expectations", Icons.Outlined.HealthAndSafety) {
            Text(explanation.recovery, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (explanation.warningSigns.isNotEmpty()) {
        ExpandableSection("Warning signs", Icons.Outlined.Warning,
            tint = MaterialTheme.semantic.riskRed) {
            Text("Seek medical attention if you notice:",
                style = MaterialTheme.typography.bodyMedium)
            explanation.warningSigns.forEach {
                Text("•  $it", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    icon: ImageVector,
    initiallyExpanded: Boolean = false,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}
