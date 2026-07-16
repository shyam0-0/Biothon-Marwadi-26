package com.medfusion.ai.ui.consultation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.StateContainer
import com.medfusion.ai.ui.components.UrgencyChip
import com.medfusion.ai.viewmodel.ConsultUiState
import com.medfusion.ai.viewmodel.DoctorConsultationViewModel

/**
 * Doctor consultation workspace (Phase 2): AI pre-read + reports, modular video
 * join, and notes → digital prescription → approval (which generates the care plan).
 */
@Composable
fun DoctorConsultationScreen(
    onCompleted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorConsultationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Consultation", onBack = onBack) { padding ->
        StateContainer(
            state = state.appointment,
            modifier = Modifier.padding(padding),
            onRetry = viewModel::load,
        ) { appointment ->
            ConsultationBody(
                appointment = appointment,
                state = state,
                onJoinCall = viewModel::joinCall,
                onNotesChange = viewModel::onNotesChange,
                onDiagnosisChange = viewModel::onDiagnosisChange,
                onAdviceChange = viewModel::onAdviceChange,
                onAddMedication = viewModel::addMedication,
                onRemoveMedication = viewModel::removeMedication,
                onComplete = viewModel::completeConsultation,
                onDone = onCompleted,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ConsultationBody(
    appointment: Appointment,
    state: ConsultUiState,
    onJoinCall: () -> Unit,
    onNotesChange: (String) -> Unit,
    onDiagnosisChange: (String) -> Unit,
    onAdviceChange: (String) -> Unit,
    onAddMedication: (Medication) -> Unit,
    onRemoveMedication: (Int) -> Unit,
    onComplete: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Patient + AI pre-read
        MedFusionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(appointment.patientName.ifBlank { "Patient" },
                        style = MaterialTheme.typography.titleMedium)
                    Text("${appointment.date} • ${appointment.timeSlot}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                UrgencyChip(level = appointment.urgencyLevel)
            }
            if (appointment.message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Patient note: ${appointment.message}",
                    style = MaterialTheme.typography.bodyMedium)
            }
            AiPreRead(state.aiCase)
        }

        // AI Consultation Brief (Phase 6): a 15-second preparation summary.
        state.brief?.let { ConsultationBriefCard(it) }

        RecoveryHistory(state)

        PrimaryButton(text = "Join Video Call", leadingIcon = Icons.Outlined.Videocam, onClick = onJoinCall)

        if (state.completed) {
            CompletedCard(onDone)
            return@Column
        }

        // Linear clinical workflow (Phase 6): diagnosis → notes → prescription.
        SectionLabel("Diagnosis")
        OutlinedTextField(
            value = state.diagnosis,
            onValueChange = onDiagnosisChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Acute bronchitis") },
            singleLine = true,
            isError = state.error != null && state.diagnosis.isBlank(),
            shape = MaterialTheme.shapes.medium,
        )

        SectionLabel("Consultation notes")
        OutlinedTextField(
            value = state.notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            placeholder = { Text("Observations from the consultation…") },
            shape = MaterialTheme.shapes.medium,
        )

        // Prescription
        SectionLabel("Prescription")
        state.medications.forEachIndexed { index, med ->
            MedFusionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(med.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${med.dosage} • ${med.timing}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onRemoveMedication(index) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove")
                    }
                }
            }
        }
        MedicationEditor(onAdd = onAddMedication)

        // Advice
        SectionLabel("Advice")
        OutlinedTextField(
            value = state.advice,
            onValueChange = onAdviceChange,
            modifier = Modifier.fillMaxWidth().height(100.dp),
            placeholder = { Text("Home care, lifestyle advice, follow-up guidance…") },
            shape = MaterialTheme.shapes.medium,
        )

        state.error?.let {
            Text(it.userMessage, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            text = "Approve & Generate Care Plan",
            loading = state.submitting,
            onClick = onComplete,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * AI Consultation Brief (Phase 6): one card, readable in ~15 seconds. A clinical
 * preparation summary only — never a diagnosis, never a conversation.
 */
@Composable
private fun ConsultationBriefCard(brief: com.medfusion.ai.viewmodel.ConsultationBrief) {
    MedFusionCard {
        Text("AI Consultation Brief", style = MaterialTheme.typography.titleMedium)
        brief.redFlagNote?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.semantic.riskRed)
        }
        BriefRow("Complaint", brief.complaint)
        BriefRow("Symptoms", brief.symptoms)
        BriefRow("Localization", brief.locations.joinToString("; "))
        BriefRow("Top conditions", brief.topConditions.joinToString(", ") +
            (brief.severityLabel?.let { " • $it severity" } ?: ""))
        BriefRow("Previous diagnosis", brief.previousDiagnosis.orEmpty())
        BriefRow("Medications", brief.medications.joinToString(", "))
        BriefRow("Allergies", brief.allergies.joinToString(", "))
        BriefRow("Reports", brief.reportsSummary.orEmpty())
        Spacer(Modifier.height(4.dp))
        Text("Preparation summary from patient records and AI analysis — not a diagnosis.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BriefRow(label: String, value: String) {
    if (value.isBlank()) return
    Spacer(Modifier.height(4.dp))
    Text("$label: $value", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AiPreRead(aiCase: Case?) {
    if (aiCase == null) return
    Spacer(Modifier.height(12.dp))
    Text("AI pre-read", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    Text(aiCase.fusionResult?.findings ?: "Analysis pending.",
        style = MaterialTheme.typography.bodyMedium)
    if (aiCase.symptomsText.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text("Reported symptoms: ${aiCase.symptomsText}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    // Body-map localization from the patient (Phase 5.6).
    if (aiCase.symptomLocations.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        com.medfusion.ai.ui.symptom.SymptomMapSummary(aiCase.symptomLocations)
    }
    val xray = aiCase.xrayUrl
    val lab = aiCase.labReportUrl
    if (xray != null || lab != null) {
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            xray?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "X-ray",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            lab?.let {
                Box(
                    Modifier.size(80.dp).clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Description, contentDescription = "Lab report") }
            }
        }
    }
}

@Composable
private fun RecoveryHistory(state: ConsultUiState) {
    if (state.recentLogs.isEmpty() && state.progress == null) return
    MedFusionCard {
        Text("Recovery history", style = MaterialTheme.typography.titleMedium)
        state.progress?.let {
            Spacer(Modifier.height(4.dp))
            Text(it.status, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Text(it.summary, style = MaterialTheme.typography.bodyMedium)
        }
        if (state.recentLogs.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            state.recentLogs.take(5).forEach { log ->
                Text(
                    "${log.date}: sleep ${log.sleepHours}h • pain ${log.painLevel}/10 • " +
                        "mood ${log.mood.label} • med ${if (log.medicationTaken) "✓" else "✗"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MedicationEditor(onAdd: (Medication) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var timing by remember { mutableStateOf("") }

    MedFusionCard {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Medicine name") }, shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dosage, onValueChange = { dosage = it },
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text("Dosage") }, shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = timing, onValueChange = { timing = it },
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text("Timing") }, shape = MaterialTheme.shapes.medium,
            )
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Add medicine",
            onClick = {
                onAdd(Medication(name.trim(), dosage.trim(), timing.trim()))
                name = ""; dosage = ""; timing = ""
            },
        )
    }
}

@Composable
private fun CompletedCard(onDone: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    InfoBanner(text = "Consultation completed. The prescription and care plan are now available to the patient.")
    Spacer(Modifier.height(12.dp))
    PrimaryButton(text = "Done", onClick = onDone)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}
