package com.medfusion.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.LiveVitalsCardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live IoT vitals card (Phase 7.4) — shared by the patient dashboard and the
 * doctor's patient view so both read the same [LiveVitalsCardState] shape.
 * Updates automatically as the underlying Firestore document changes; no
 * refresh action exists here by design.
 */
@Composable
fun LiveVitalsCard(
    state: LiveVitalsCardState,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    MedFusionCard(modifier = modifier, contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text("Live vitals", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))

        when (state) {
            is LiveVitalsCardState.Loading -> Text(
                "Waiting for live vitals...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is LiveVitalsCardState.Empty -> Text(
                emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is LiveVitalsCardState.Error -> InlineErrorCard(error = state.error)
            is LiveVitalsCardState.Data -> {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    VitalReading(label = "Heart rate", value = "${state.vitals.heartRate} bpm")
                    VitalReading(label = "SpO₂", value = "${state.vitals.spo2}%")
                }
                state.vitals.updatedAtMillis?.let { millis ->
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Last updated ${formatTime(millis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VitalReading(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
