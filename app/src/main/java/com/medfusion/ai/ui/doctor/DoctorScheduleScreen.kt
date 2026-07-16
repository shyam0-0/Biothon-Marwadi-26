package com.medfusion.ai.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.UrgencyChip
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.DoctorScheduleViewModel

/**
 * Doctor schedule (Phase 6): appointments grouped Today / Upcoming / Completed
 * for quick review. Informational only — no calendar sync, no notifications.
 */
@Composable
fun DoctorScheduleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorScheduleViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Schedule", onBack = onBack) { padding ->
        val empty = groups.today.isEmpty() && groups.upcoming.isEmpty() && groups.completed.isEmpty()
        if (empty) {
            EmptyView(
                title = "No appointments scheduled",
                subtitle = "Your consultation schedule appears here.",
                icon = Icons.Outlined.EventNote,
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Sizes.screenPadding,
                    end = Sizes.screenPadding,
                    top = Spacing.md,
                    bottom = Spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                scheduleGroup("Today", groups.today)
                scheduleGroup("Upcoming", groups.upcoming)
                scheduleGroup("Completed", groups.completed)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.scheduleGroup(
    title: String,
    appointments: List<Appointment>,
) {
    if (appointments.isEmpty()) return
    item(key = "header-$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.sm),
        )
    }
    items(appointments, key = { it.id }) { appointment ->
        ScheduleRow(appointment)
    }
}

@Composable
private fun ScheduleRow(appointment: Appointment) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(appointment.patientName.ifBlank { "Patient" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    "${appointment.date} at ${appointment.timeSlot}" +
                        (appointment.specialty?.let { " • $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    appointment.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (appointment.status == AppointmentStatus.COMPLETED)
                        MaterialTheme.semantic.riskGreen
                    else MaterialTheme.colorScheme.primary,
                )
            }
            if (appointment.status != AppointmentStatus.COMPLETED) {
                UrgencyChip(level = appointment.urgencyLevel)
            }
        }
    }
}
