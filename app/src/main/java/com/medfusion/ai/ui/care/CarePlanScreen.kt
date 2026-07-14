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
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.CarePlanViewModel
import kotlin.math.roundToInt

/**
 * Adaptive care companion: shows the doctor-set plan, adaptive suggestions, and a
 * daily check-in. Lifestyle suggestions are applied directly; medication-related
 * ones are held for doctor approval.
 */
@Composable
fun CarePlanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarePlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Care Plan", onBack = onBack) { padding ->
        if (state.loading) {
            LoadingView(modifier = Modifier.padding(padding), message = "Loading your care plan…")
            return@MedFusionScaffold
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            state.plan?.let { CarePlanSection(it) }

            state.suggestions.forEach { SuggestionCard(it) }

            CheckInSection(
                submitting = state.submitting,
                onSubmit = viewModel::submitCheckIn,
            )

            state.error?.let {
                Text(it.userMessage, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (state.checkInDone) {
                InfoBanner(text = "Thanks! Today's check-in is saved. We'll adapt your plan as needed.")
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun CarePlanSection(plan: CarePlan) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Your care plan", style = MaterialTheme.typography.titleLarge)
        plan.note?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(it, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (plan.medications.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Text("Medications", style = MaterialTheme.typography.labelLarge)
            plan.medications.forEach { med ->
                Row(Modifier.padding(top = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Medication, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("${med.name} • ${med.dosage} • ${med.timing}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (plan.activityGoals.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            Text("Today's goals", style = MaterialTheme.typography.labelLarge)
            plan.activityGoals.forEach { goal ->
                Text("• $goal", style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.xs))
            }
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
    onSubmit: (sleepHours: Double?, activity: ActivityLevel?, mood: Mood?) -> Unit,
) {
    var sleep by remember { mutableStateOf(7f) }
    var activity by remember { mutableStateOf<ActivityLevel?>(null) }
    var mood by remember { mutableStateOf<Mood?>(null) }

    MedFusionCard(contentPadding = Spacing.lg) {
        Text("Daily check-in", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Bedtime, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.sm))
            Text("Sleep: ${sleep.roundToInt()} hours", style = MaterialTheme.typography.bodyLarge)
        }
        Slider(value = sleep, onValueChange = { sleep = it }, valueRange = 0f..12f, steps = 11)

        Spacer(Modifier.height(Spacing.sm))
        Text("Activity level", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ActivityLevel.entries.forEach { level ->
                FilterChip(
                    selected = activity == level,
                    onClick = { activity = level },
                    label = { Text(level.label) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text("Mood", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Mood.entries.forEach { m ->
                FilterChip(
                    selected = mood == m,
                    onClick = { mood = m },
                    label = { Text(m.label) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = "Save check-in",
            loading = submitting,
            enabled = activity != null && mood != null,
            onClick = { onSubmit(sleep.toDouble(), activity, mood) },
        )
    }
}
