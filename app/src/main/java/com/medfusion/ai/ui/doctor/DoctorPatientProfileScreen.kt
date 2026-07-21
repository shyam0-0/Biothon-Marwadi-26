package com.medfusion.ai.ui.doctor

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.ui.components.ErrorView
import com.medfusion.ai.ui.components.LiveVitalsCard
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.SeverityChip
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.DoctorPatientProfileViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Read-only patient record for the doctor (Phase 6): passport, medical history,
 * AI analyses, consultations, care plan and recovery progress. Review only —
 * historical records are never editable here.
 */
@Composable
fun DoctorPatientProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorPatientProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Phase 7.5 temporary diagnostic: confirms the UI actually recomposes
    // when the ViewModel's liveVitals state changes.
    LaunchedEffect(state.liveVitals) {
        Log.d("LiveVitals", "[UI] DoctorPatientProfileScreen received state: ${state.liveVitals}")
    }

    MedFusionScaffold(title = state.patientName.ifBlank { "Patient" }, onBack = onBack) { padding ->
        when {
            state.loading -> LoadingView(
                modifier = Modifier.padding(padding),
                message = "Loading patient record…",
            )
            state.error != null ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorView(error = state.error!!, onRetry = viewModel::load)
                }
            else -> Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                LiveVitalsCard(state = state.liveVitals, emptyMessage = "No vitals received yet.")

                state.passport?.let { PassportSummary(it) }

                state.progress?.let { progress ->
                    Section("Recovery progress", Icons.Outlined.MonitorHeart,
                        tint = if (progress.followUpRecommended) MaterialTheme.semantic.riskRed
                        else MaterialTheme.semantic.riskGreen) {
                        Text(progress.status,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(progress.summary, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.recentLogs.isNotEmpty()) {
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "Latest check-in (${state.recentLogs.first().date}): pain " +
                                    "${state.recentLogs.first().painLevel}/10, mood " +
                                    state.recentLogs.first().mood.label.lowercase(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                state.carePlan?.let { plan ->
                    Section("Care plan", Icons.Outlined.MedicalServices) {
                        plan.diagnosis?.let {
                            Text("Diagnosis: $it", style = MaterialTheme.typography.bodyMedium)
                        }
                        plan.medications.forEach {
                            Text("•  ${it.name} ${it.dosage} • ${it.timing}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = Spacing.xs))
                        }
                        plan.followUpDate?.let {
                            Text("Follow-up: $it", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.xs))
                        }
                    }
                }

                if (state.consultations.isNotEmpty()) {
                    Section("Previous consultations", Icons.Outlined.EventNote) {
                        state.consultations.forEach { appt ->
                            Text("${appt.date} — ${appt.diagnosis ?: "No diagnosis recorded"}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = Spacing.xs))
                            appt.doctorNotes?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (state.aiHistory.isNotEmpty()) {
                    Section("Previous AI analyses", Icons.Outlined.History) {
                        state.aiHistory.forEach { record ->
                            Row(
                                modifier = Modifier.padding(top = Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(formatDate(record.dateMillis),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val top = record.conditions.maxByOrNull { it.confidence }
                                    Text(
                                        top?.let { "${it.name} (${it.confidence}%)" }
                                            ?: record.symptoms.take(60),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    if (record.locations.isNotEmpty()) {
                                        Text(record.locations.joinToString("; "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                SeverityChip(severity = record.severity)
                            }
                        }
                    }
                }

                if (state.timeline.isNotEmpty()) {
                    Section("Medical timeline", Icons.Outlined.History) {
                        state.timeline.take(10).forEach { event ->
                            Text(
                                "${formatDate(event.dateMillis)} — ${event.title}" +
                                    (event.detail.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = Spacing.xs),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun PassportSummary(passport: PatientPassport) {
    Section("Patient passport", Icons.Outlined.Badge) {
        val vitals = listOfNotNull(
            passport.age?.let { "Age $it" },
            passport.gender.takeIf { it.isNotBlank() },
            passport.bloodGroup.takeIf { it.isNotBlank() },
            passport.bmi?.let { "BMI $it" },
        )
        if (vitals.isNotEmpty()) {
            Text(vitals.joinToString(" • "), style = MaterialTheme.typography.bodyMedium)
        }
        InfoRow("Allergies", passport.allergies)
        InfoRow("Current medications", passport.currentMedications)
        InfoRow("Chronic diseases", passport.chronicDiseases)
        InfoRow("Previous diagnoses", passport.previousDiagnoses)
        InfoRow("Previous surgeries", passport.previousSurgeries)
        val lifestyle = listOfNotNull(
            "Smoking".takeIf { passport.smoker },
            "Alcohol".takeIf { passport.alcohol },
            "Pregnancy".takeIf { passport.pregnant },
        )
        if (lifestyle.isNotEmpty()) InfoRow("Lifestyle factors", lifestyle)
        if (passport.emergencyContact.isNotBlank()) {
            Text("Emergency contact: ${passport.emergencyContact}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

@Composable
private fun InfoRow(label: String, values: List<String>) {
    Text(label, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs))
    Text(
        if (values.isEmpty()) "None recorded" else values.joinToString(),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun Section(
    title: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint)
            Spacer(Modifier.width(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.xs))
        content()
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
