package com.medfusion.ai.ui.passport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.model.TimelineEventType
import com.medfusion.ai.ui.components.ErrorView
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.SeverityChip
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.PatientPassportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Patient Passport (Phase 5): the patient's digital health record. Profile,
 * medical summary, risk factors, current care plan, upcoming appointment, stored
 * AI sessions, latest prescription and the automatic medical timeline — every
 * module contributes here, nothing is entered manually except the profile itself.
 */
@Composable
fun PatientPassportScreen(
    onBack: () -> Unit,
    onOpenCarePlan: () -> Unit,
    onOpenAppointments: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientPassportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Patient Passport", onBack = onBack) { padding ->
        when {
            state.loading -> LoadingView(
                modifier = Modifier.padding(padding),
                message = "Opening your health record…",
            )
            state.error != null && state.passport == null ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorView(error = state.error!!, onRetry = viewModel::load)
                }
            else -> {
                val passport = state.passport ?: return@MedFusionScaffold
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    if (state.editing) {
                        PassportEditForm(
                            passport = passport,
                            saving = state.saving,
                            onSave = viewModel::savePassport,
                            onCancel = viewModel::cancelEditing,
                        )
                    } else {
                        ProfileCard(passport, onEdit = viewModel::startEditing)
                        MedicalSummaryCard(passport)
                        if (passport.riskFactors.isNotEmpty()) RiskFactorsCard(passport.riskFactors)
                    }

                    state.carePlan?.let { plan ->
                        SectionCard(
                            icon = Icons.Outlined.MedicalServices,
                            title = "Current care plan",
                            onClick = onOpenCarePlan,
                        ) {
                            plan.diagnosis?.let { Text("Diagnosis: $it", style = MaterialTheme.typography.bodyMedium) }
                            plan.doctorName?.let { Text("Doctor: $it", style = MaterialTheme.typography.bodyMedium) }
                            Text(
                                "${plan.medications.size} medication(s)" +
                                    (plan.followUpDate?.let { " • follow-up $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    state.nextAppointment?.let { appt ->
                        SectionCard(
                            icon = Icons.Outlined.EventAvailable,
                            title = "Upcoming appointment",
                            onClick = onOpenAppointments,
                        ) {
                            Text("${appt.doctorName} • ${appt.date} at ${appt.timeSlot}",
                                style = MaterialTheme.typography.bodyMedium)
                            Text(appt.status.label, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    state.latestPrescription?.let { rx ->
                        SectionCard(icon = Icons.Outlined.Medication, title = "Latest prescription") {
                            Text("${rx.diagnosis} — ${rx.doctorName}", style = MaterialTheme.typography.bodyMedium)
                            rx.medications.take(3).forEach {
                                Text("•  ${it.name} ${it.dosage} • ${it.timing}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = Spacing.xs))
                            }
                        }
                    }

                    if (state.aiHistory.isNotEmpty()) {
                        Text("Previous AI sessions", style = MaterialTheme.typography.titleMedium)
                        state.aiHistory.forEach { record -> AiSessionCard(record) }
                    }

                    if (state.timeline.isNotEmpty()) {
                        Text("Medical timeline", style = MaterialTheme.typography.titleMedium)
                        TimelineCard(state.timeline)
                    }

                    Spacer(Modifier.height(Spacing.lg))
                }
            }
        }
    }
}

// --- Profile & medical summary ----------------------------------------------

@Composable
private fun ProfileCard(passport: PatientPassport, onEdit: () -> Unit) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                passport.fullName.ifBlank { "Your profile" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        if (passport.isEmpty) {
            Text(
                "Complete your health profile so every AI analysis and consultation understands your history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val vitals = listOfNotNull(
                passport.age?.let { "Age $it" },
                passport.gender.takeIf { it.isNotBlank() },
                passport.bloodGroup.takeIf { it.isNotBlank() },
                passport.heightCm?.let { "${it.toInt()} cm" },
                passport.weightKg?.let { "${it.toInt()} kg" },
                passport.bmi?.let { "BMI $it" },
            )
            if (vitals.isNotEmpty()) {
                Text(vitals.joinToString(" • "), style = MaterialTheme.typography.bodyMedium)
            }
            if (passport.contactNumber.isNotBlank()) {
                Text("Contact: ${passport.contactNumber}", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.xs))
            }
            if (passport.emergencyContact.isNotBlank()) {
                Text("Emergency: ${passport.emergencyContact}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.semantic.riskRed, modifier = Modifier.padding(top = Spacing.xs))
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton(text = if (passport.isEmpty) "Set up my profile" else "Edit profile", onClick = onEdit)
    }
}

@Composable
private fun MedicalSummaryCard(passport: PatientPassport) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.HealthAndSafety, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text("Medical summary", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        SummaryRow("Allergies", passport.allergies)
        SummaryRow("Chronic diseases", passport.chronicDiseases)
        SummaryRow("Current medications", passport.currentMedications)
        SummaryRow("Previous diagnoses", passport.previousDiagnoses)
        SummaryRow("Previous surgeries", passport.previousSurgeries)
        SummaryRow("Vaccinations", passport.vaccinations)
        val lifestyle = listOfNotNull(
            "Smoking".takeIf { passport.smoker },
            "Alcohol".takeIf { passport.alcohol },
            "Pregnancy".takeIf { passport.pregnant },
        )
        SummaryRow("Lifestyle factors", lifestyle)
    }
}

@Composable
private fun SummaryRow(label: String, values: List<String>) {
    Text(label, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs))
    Text(
        if (values.isEmpty()) "None recorded" else values.joinToString(),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun RiskFactorsCard(riskFactors: List<String>) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.semantic.riskRed)
            Spacer(Modifier.width(Spacing.sm))
            Text("Risk factors", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        riskFactors.forEach {
            Text("•  $it", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = Spacing.xs))
        }
    }
}

// --- Edit form ----------------------------------------------------------------

@Composable
private fun PassportEditForm(
    passport: PatientPassport,
    saving: Boolean,
    onSave: (PatientPassport) -> Unit,
    onCancel: () -> Unit,
) {
    var fullName by remember { mutableStateOf(passport.fullName) }
    var age by remember { mutableStateOf(passport.age?.toString().orEmpty()) }
    var gender by remember { mutableStateOf(passport.gender) }
    var bloodGroup by remember { mutableStateOf(passport.bloodGroup) }
    var height by remember { mutableStateOf(passport.heightCm?.toString().orEmpty()) }
    var weight by remember { mutableStateOf(passport.weightKg?.toString().orEmpty()) }
    var contact by remember { mutableStateOf(passport.contactNumber) }
    var emergency by remember { mutableStateOf(passport.emergencyContact) }
    var allergies by remember { mutableStateOf(passport.allergies.joinToString(", ")) }
    var chronic by remember { mutableStateOf(passport.chronicDiseases.joinToString(", ")) }
    var meds by remember { mutableStateOf(passport.currentMedications.joinToString(", ")) }
    var diagnoses by remember { mutableStateOf(passport.previousDiagnoses.joinToString(", ")) }
    var surgeries by remember { mutableStateOf(passport.previousSurgeries.joinToString(", ")) }
    var vaccinations by remember { mutableStateOf(passport.vaccinations.joinToString(", ")) }
    var smoker by remember { mutableStateOf(passport.smoker) }
    var alcohol by remember { mutableStateOf(passport.alcohol) }
    var pregnant by remember { mutableStateOf(passport.pregnant) }

    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Edit health profile", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        EditField("Full name", fullName) { fullName = it }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(Modifier.weight(1f)) { EditField("Age", age) { age = it } }
            Box(Modifier.weight(1f)) { EditField("Gender", gender) { gender = it } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(Modifier.weight(1f)) { EditField("Blood group", bloodGroup) { bloodGroup = it } }
            Box(Modifier.weight(1f)) { EditField("Height (cm)", height) { height = it } }
            Box(Modifier.weight(1f)) { EditField("Weight (kg)", weight) { weight = it } }
        }
        EditField("Contact number", contact) { contact = it }
        EditField("Emergency contact", emergency) { emergency = it }
        EditField("Allergies (comma-separated)", allergies) { allergies = it }
        EditField("Chronic diseases (comma-separated)", chronic) { chronic = it }
        EditField("Current medications (comma-separated)", meds) { meds = it }
        EditField("Previous diagnoses (comma-separated)", diagnoses) { diagnoses = it }
        EditField("Previous surgeries (comma-separated)", surgeries) { surgeries = it }
        EditField("Vaccinations (comma-separated)", vaccinations) { vaccinations = it }
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(selected = smoker, onClick = { smoker = !smoker }, label = { Text("Smoking") })
            FilterChip(selected = alcohol, onClick = { alcohol = !alcohol }, label = { Text("Alcohol") })
            FilterChip(selected = pregnant, onClick = { pregnant = !pregnant }, label = { Text("Pregnancy") })
        }
        Spacer(Modifier.height(Spacing.md))
        PrimaryButton(
            text = "Save profile",
            loading = saving,
            onClick = {
                onSave(
                    passport.copy(
                        fullName = fullName.trim(),
                        age = age.trim().toIntOrNull(),
                        gender = gender.trim(),
                        bloodGroup = bloodGroup.trim(),
                        heightCm = height.trim().toDoubleOrNull(),
                        weightKg = weight.trim().toDoubleOrNull(),
                        contactNumber = contact.trim(),
                        emergencyContact = emergency.trim(),
                        allergies = allergies.toCleanList(),
                        chronicDiseases = chronic.toCleanList(),
                        currentMedications = meds.toCleanList(),
                        previousDiagnoses = diagnoses.toCleanList(),
                        previousSurgeries = surgeries.toCleanList(),
                        vaccinations = vaccinations.toCleanList(),
                        smoker = smoker,
                        alcohol = alcohol,
                        pregnant = pregnant,
                    )
                )
            },
        )
        Spacer(Modifier.height(Spacing.sm))
        SecondaryButton(text = "Cancel", onClick = onCancel)
    }
}

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
        label = { Text(label) },
        shape = MaterialTheme.shapes.medium,
        singleLine = true,
    )
}

private fun String.toCleanList(): List<String> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }

// --- AI history & timeline ------------------------------------------------------

@Composable
private fun AiSessionCard(record: AiConsultationRecord) {
    var expanded by remember { mutableStateOf(false) }
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.History, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(formatDate(record.dateMillis), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val top = record.conditions.maxByOrNull { it.confidence }
                Text(
                    top?.let { "${it.name} (${it.confidence}%)" } ?: record.symptoms.take(60),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            SeverityChip(severity = record.severity)
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Spacer(Modifier.height(Spacing.sm))
            Text("Symptoms: ${record.symptoms}", style = MaterialTheme.typography.bodyMedium)
            if (record.summary.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(record.summary, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            record.conditions.forEach {
                Text("•  ${it.name} — ${it.confidence}%", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.xs))
            }
            if (record.locations.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Text("Locations: ${record.locations.joinToString("; ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (record.recommendedTests.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Text("Tests: ${record.recommendedTests.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            record.recommendedSpecialist?.let {
                Text("Specialist: $it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TimelineCard(events: List<TimelineEvent>) {
    MedFusionCard(contentPadding = Spacing.lg) {
        events.forEachIndexed { index, event ->
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(dotColor(event.type), CircleShape)
                    )
                    if (index != events.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(44.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.padding(bottom = Spacing.sm)) {
                    Text(formatDate(event.dateMillis), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(event.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    if (event.detail.isNotBlank()) {
                        Text(event.detail, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun dotColor(type: TimelineEventType) = when (type) {
    TimelineEventType.AI_ANALYSIS -> MaterialTheme.colorScheme.primary
    TimelineEventType.REPORT_UPLOADED -> MaterialTheme.colorScheme.tertiary
    TimelineEventType.APPOINTMENT_BOOKED, TimelineEventType.FOLLOW_UP -> MaterialTheme.semantic.riskYellow
    TimelineEventType.DOCTOR_CONSULTATION, TimelineEventType.PRESCRIPTION -> MaterialTheme.semantic.riskRed
    TimelineEventType.CARE_PLAN_STARTED, TimelineEventType.DAILY_CHECK_IN -> MaterialTheme.semantic.riskGreen
}

// --- Shared -----------------------------------------------------------------

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    MedFusionCard(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        contentPadding = Spacing.lg,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        content()
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))
