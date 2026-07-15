package com.medfusion.ai.ui.care

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.CarePlanViewModel
import kotlin.math.roundToInt

/**
 * Care companion (Phase 3): a plan exists only after doctor approval or an accepted
 * AI wellness plan. Shows the dynamic plan, adaptive suggestions, AI recovery
 * progress, and a detailed daily check-in.
 */
@Composable
fun CarePlanScreen(
    onBack: () -> Unit,
    onBookFollowUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarePlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Care Plan", onBack = onBack) { padding ->
        when {
            state.loading ->
                LoadingView(modifier = Modifier.padding(padding), message = "Loading your care plan…")

            state.plan == null ->
                WellnessGate(
                    generating = state.generatingPlan,
                    error = state.error?.userMessage,
                    onAccept = viewModel::acceptWellnessPlan,
                    modifier = modifier.padding(padding),
                )

            else ->
                CareContent(
                    plan = state.plan!!,
                    suggestions = state.suggestions,
                    progress = state.progress,
                    submitting = state.submitting,
                    checkInDone = state.checkInDone,
                    errorText = state.error?.userMessage,
                    onSubmit = viewModel::submitCheckIn,
                    onBookFollowUp = onBookFollowUp,
                    modifier = modifier.padding(padding),
                )
        }
    }
}

@Composable
private fun WellnessGate(
    generating: Boolean,
    error: String?,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyView(
            title = "No care plan yet",
            subtitle = "A care plan is created after a doctor consultation. For minor symptoms, you can accept an AI wellness plan to guide your self-care.",
            icon = Icons.Outlined.Spa,
        )
        PrimaryButton(
            text = "Accept AI Wellness Plan",
            loading = generating,
            onClick = onAccept,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium) }
        InfoBanner(text = "An AI wellness plan is for minor conditions only and is not a substitute for a doctor's advice.")
    }
}

@Composable
private fun CareContent(
    plan: CarePlan,
    suggestions: List<CareSuggestion>,
    progress: ProgressAnalysis?,
    submitting: Boolean,
    checkInDone: Boolean,
    errorText: String?,
    onSubmit: (Double, ActivityLevel?, Mood?, Int, String, Boolean, Double?, String) -> Unit,
    onBookFollowUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PlanCard(plan)
        progress?.let { ProgressCard(it) }
        // Phase 4: recovery tracking loops back into a follow-up booking when
        // the doctor scheduled one or the AI progress analysis recommends it.
        if (plan.followUpDate != null || progress?.followUpRecommended == true) {
            SecondaryButton(text = "Book follow-up appointment", onClick = onBookFollowUp)
        }
        suggestions.forEach { SuggestionCard(it) }
        CheckInSection(submitting = submitting, onSubmit = onSubmit)
        errorText?.let { Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium) }
        if (checkInDone) {
            InfoBanner(text = "Thanks! Today's check-in is saved and your recovery trend will update.")
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun PlanCard(plan: CarePlan) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Your care plan", style = MaterialTheme.typography.titleLarge)
        plan.diagnosis?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text("Diagnosis: $it", style = MaterialTheme.typography.bodyMedium)
        }
        plan.doctorName?.takeIf { it.isNotBlank() }?.let {
            Text("Assigned doctor: $it", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (plan.medications.isNotEmpty()) {
            Section("Medications & reminders", Icons.Outlined.Medication)
            plan.medicineReminders.forEach { Bullet(it) }
        }
        if (plan.recoveryGoals.isNotEmpty()) {
            Section("Recovery goals", Icons.Outlined.MonitorHeart)
            plan.recoveryGoals.forEach { Bullet(it) }
        }
        if (plan.activityGoals.isNotEmpty()) {
            Section("Daily goals", Icons.Outlined.FitnessCenter)
            plan.activityGoals.forEach { Bullet(it) }
        }
        if (plan.lifestyle.isNotEmpty()) {
            Section("Lifestyle", Icons.Outlined.Spa)
            plan.lifestyle.forEach { Bullet(it) }
        }
        plan.hydration?.let { LineItem("Hydration", it, Icons.Outlined.LocalDrink) }
        plan.exercise?.let { LineItem("Exercise", it, Icons.Outlined.FitnessCenter) }
        plan.sleep?.let { LineItem("Sleep", it, Icons.Outlined.Bedtime) }
        plan.followUpDate?.let {
            Spacer(Modifier.height(Spacing.sm))
            Text("Next follow-up: $it", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ProgressCard(progress: ProgressAnalysis) {
    val warn = progress.followUpRecommended
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.MonitorHeart, contentDescription = null,
                tint = if (warn) MaterialTheme.semantic.riskRed else MaterialTheme.semantic.riskGreen)
            Spacer(Modifier.width(Spacing.sm))
            Text(progress.status, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(progress.summary, style = MaterialTheme.typography.bodyMedium)
        if (warn) {
            Spacer(Modifier.height(Spacing.sm))
            Text("A follow-up appointment is recommended.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.semantic.riskRed)
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: CareSuggestion) {
    val semantic = MaterialTheme.semantic
    MedFusionCard(contentPadding = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (suggestion.pending) Icons.Outlined.HourglassTop else Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = if (suggestion.pending) semantic.riskYellow else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                if (suggestion.pending) "Awaiting doctor approval" else "Suggestion",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(suggestion.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInSection(
    submitting: Boolean,
    onSubmit: (Double, ActivityLevel?, Mood?, Int, String, Boolean, Double?, String) -> Unit,
) {
    var sleep by remember { mutableStateOf(7f) }
    var pain by remember { mutableStateOf(0f) }
    var activity by remember { mutableStateOf<ActivityLevel?>(null) }
    var mood by remember { mutableStateOf<Mood?>(null) }
    var symptoms by remember { mutableStateOf("") }
    var medTaken by remember { mutableStateOf(false) }
    var temperature by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Daily check-in", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text("Sleep: ${sleep.roundToInt()} hours", style = MaterialTheme.typography.bodyLarge)
        }
        Slider(value = sleep, onValueChange = { sleep = it }, valueRange = 0f..12f, steps = 11)

        Text("Pain level: ${pain.roundToInt()}/10", style = MaterialTheme.typography.bodyLarge)
        Slider(value = pain, onValueChange = { pain = it }, valueRange = 0f..10f, steps = 9)

        Spacer(Modifier.height(Spacing.sm))
        Text("Activity level", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ActivityLevel.entries.forEach { level ->
                FilterChip(selected = activity == level, onClick = { activity = level },
                    label = { Text(level.label) })
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text("Mood", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Mood.entries.forEach { m ->
                FilterChip(selected = mood == m, onClick = { mood = m }, label = { Text(m.label) })
            }
        }

        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = symptoms, onValueChange = { symptoms = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Current symptoms") },
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = temperature, onValueChange = { temperature = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Temperature (°F, optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = notes, onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Additional notes (optional)") },
            shape = MaterialTheme.shapes.medium,
        )

        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = medTaken, onCheckedChange = { medTaken = it })
            Text("I took my medication today", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(Spacing.md))
        PrimaryButton(
            text = "Save check-in",
            loading = submitting,
            enabled = activity != null && mood != null,
            onClick = {
                onSubmit(
                    sleep.toDouble(), activity, mood, pain.roundToInt(),
                    symptoms, medTaken, temperature.toDoubleOrNull(), notes,
                )
            },
        )
    }
}

@Composable
private fun Section(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Spacer(Modifier.height(Spacing.md))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(Spacing.sm))
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(Spacing.xs))
}

@Composable
private fun Bullet(text: String) {
    Text("•  $text", style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = Spacing.xs))
}

@Composable
private fun LineItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Section(label, icon)
    Text(value, style = MaterialTheme.typography.bodyMedium)
}
