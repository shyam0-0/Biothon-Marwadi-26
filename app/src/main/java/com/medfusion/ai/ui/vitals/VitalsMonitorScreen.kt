package com.medfusion.ai.ui.vitals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.VitalsPhase
import com.medfusion.ai.viewmodel.VitalsViewModel

/**
 * Live (simulated) heart-rate monitor. Sustained abnormal readings raise a
 * confirm-gated, full-screen emergency alert that escalates on confirmation or
 * timeout. A demo toggle drives readings into the abnormal range.
 */
@Composable
fun VitalsMonitorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VitalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val normal = state.currentHr in 50..120

    MedFusionScaffold(title = "Vitals Monitor", onBack = onBack) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MedFusionCard(contentPadding = Spacing.lg) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Favorite,
                        contentDescription = null,
                        tint = if (normal) MaterialTheme.semantic.riskGreen else MaterialTheme.semantic.riskRed,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "Heart rate",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "${state.currentHr}",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (normal) MaterialTheme.colorScheme.onSurface else MaterialTheme.semantic.riskRed,
                )
                Text("bpm • normal 50–120", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.md))
                Sparkline(values = state.history)
            }

            MedFusionCard(contentPadding = Spacing.md) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Simulate abnormal reading", style = MaterialTheme.typography.titleMedium)
                        Text("Demo only — drives the emergency flow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = state.simulateAbnormal,
                        onCheckedChange = viewModel::setSimulateAbnormal,
                    )
                }
            }

            state.error?.let {
                Text(it.userMessage, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // Overlays for the emergency flow.
    when (val phase = state.phase) {
        is VitalsPhase.Alert -> EmergencyAlertDialog(
            heartRate = state.currentHr,
            secondsLeft = phase.secondsLeft,
            onConfirm = viewModel::confirmEmergency,
            onImFine = viewModel::imFine,
        )
        is VitalsPhase.Escalating -> EscalatingDialog()
        is VitalsPhase.Escalated -> EscalatedDialog(
            hospitalName = phase.outcome.hospital?.name,
            contactNotified = phase.outcome.contactNotified,
            onDone = viewModel::resumeMonitoring,
        )
        is VitalsPhase.Dismissed -> DismissedDialog(onDone = viewModel::resumeMonitoring)
        VitalsPhase.Monitoring -> Unit
    }
}

@Composable
private fun EmergencyAlertDialog(
    heartRate: Int,
    secondsLeft: Int,
    onConfirm: () -> Unit,
    onImFine: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Outlined.Favorite, contentDescription = null, tint = MaterialTheme.semantic.riskRed) },
        title = { Text("Unusual heart rate detected") },
        text = {
            Text(
                "Your heart rate is $heartRate bpm. Are you okay? " +
                    "We'll alert emergency services in $secondsLeft s if there's no response.",
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = { PrimaryButton(text = "Confirm Emergency", onClick = onConfirm) },
        dismissButton = { SecondaryButton(text = "I'm Fine", onClick = onImFine) },
    )
}

@Composable
private fun EscalatingDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Contacting help…") },
        text = { Text("Notifying your emergency contact and the nearest hospital.") },
        confirmButton = {},
    )
}

@Composable
private fun EscalatedDialog(hospitalName: String?, contactNotified: Boolean, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Help is on the way") },
        text = {
            Column {
                if (hospitalName != null) Text("• $hospitalName has been alerted with your location.")
                Text(if (contactNotified) "• Your emergency contact has been notified by SMS."
                     else "• No emergency contact on file — add one in your profile.")
            }
        },
        confirmButton = { PrimaryButton(text = "Done", onClick = onDone) },
    )
}

@Composable
private fun DismissedDialog(onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Glad you're okay") },
        text = { Text("We've logged this reading. Monitoring will continue.") },
        confirmButton = { PrimaryButton(text = "Continue", onClick = onDone) },
    )
}

@Composable
private fun Sparkline(values: List<Int>) {
    val line = MaterialTheme.colorScheme.primary
    if (values.size < 2) {
        Spacer(Modifier.height(56.dp))
        return
    }
    Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        val minV = (values.min() - 5).coerceAtLeast(0)
        val maxV = values.max() + 5
        val range = (maxV - minV).coerceAtLeast(1)
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, v ->
            val x = index * stepX
            val y = size.height - ((v - minV).toFloat() / range) * size.height
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = line,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
    }
}
