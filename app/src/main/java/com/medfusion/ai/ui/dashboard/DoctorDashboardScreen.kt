package com.medfusion.ai.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import com.medfusion.ai.ui.notifications.NotificationBell
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.ui.components.ActionCard
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.DoctorDashboardViewModel
import com.medfusion.ai.viewmodel.SessionViewModel
import java.time.LocalDate

/**
 * Doctor home (Phase 6): a clinical dashboard — the day at a glance (today /
 * waiting / completed), the next consultation, high-priority patients, and quick
 * access to the patient directory and schedule — above the live urgency-sorted
 * queue with Accept/Reschedule/Decline and the AI pre-read.
 */
@Composable
fun DoctorDashboardScreen(
    onLoggedOut: () -> Unit,
    onOpenConsultation: (appointmentId: String) -> Unit,
    onOpenPatients: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    viewModel: DoctorDashboardViewModel = hiltViewModel(),
) {
    val user by sessionViewModel.currentUser.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val cases by viewModel.cases.collectAsStateWithLifecycle()
    val name = user?.fullName?.trim()?.takeIf { it.isNotBlank() }

    var expandedId by remember { mutableStateOf<String?>(null) }
    var reschedulingId by remember { mutableStateOf<String?>(null) }

    // Reschedule flow (date + time slot) lifted to screen level.
    RescheduleFlow(
        appointmentId = reschedulingId,
        initialDateMillis = System.currentTimeMillis(),
        onDismiss = { reschedulingId = null },
        onConfirm = { id, millis, slot ->
            viewModel.reschedule(id, millis, slot)
            reschedulingId = null
        },
    )

    MedFusionScaffold(
        title = "Doctor Dashboard",
        actions = {
            NotificationBell(onOpen = onOpenNotifications)
            IconButton(onClick = onOpenProfile) {
                Icon(Icons.Outlined.AccountCircle, contentDescription = "My profile")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
            IconButton(onClick = { sessionViewModel.logout(); onLoggedOut() }) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = Sizes.screenPadding,
                end = Sizes.screenPadding,
                top = Spacing.md,
                bottom = Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { Greeting(name) }
            item { DayOverview(queue) }

            val highPriority = queue.filter {
                it.urgencyLevel == UrgencyLevel.RED &&
                    it.status != AppointmentStatus.COMPLETED &&
                    it.status != AppointmentStatus.DECLINED
            }
            if (highPriority.isNotEmpty()) {
                item { HighPriorityCard(highPriority, onOpenConsultation) }
            }

            item {
                ActionCard(
                    title = "Patients",
                    subtitle = "Search patients and review their records",
                    icon = Icons.Outlined.Groups,
                    onClick = onOpenPatients,
                )
            }
            item {
                ActionCard(
                    title = "Schedule",
                    subtitle = "Today, upcoming and completed consultations",
                    icon = Icons.Outlined.EventNote,
                    onClick = onOpenSchedule,
                )
            }

            if (queue.isEmpty()) {
                item {
                    EmptyView(
                        title = "No patient requests yet",
                        subtitle = "Incoming appointments appear here, most urgent first.",
                        icon = Icons.Outlined.Inbox,
                        modifier = Modifier.height(androidx.compose.ui.unit.Dp(260f)),
                    )
                }
            } else {
                item {
                    Text("Patient queue", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.sm))
                }
                items(queue, key = { it.id }) { appointment ->
                    val isExpanded = expandedId == appointment.id
                    AppointmentQueueCard(
                        appointment = appointment,
                        expanded = isExpanded,
                        caseState = appointment.caseId?.let { cases[it] },
                        onToggle = {
                            expandedId = if (isExpanded) null else appointment.id
                            if (!isExpanded) appointment.caseId?.let(viewModel::loadCase)
                        },
                        onAccept = { viewModel.accept(appointment.id) },
                        onReschedule = { reschedulingId = appointment.id },
                        onDecline = { viewModel.decline(appointment.id) },
                        onOpenConsultation = { onOpenConsultation(appointment.id) },
                    )
                }
            }
        }
    }
}

/** The day at a glance: today / waiting / completed + the next consultation. */
@Composable
private fun DayOverview(queue: List<Appointment>) {
    val today = LocalDate.now().toString()
    val active = queue.filter { it.status != AppointmentStatus.DECLINED }
    val todayCount = active.count { it.date == today && it.status != AppointmentStatus.COMPLETED }
    val waiting = active.count {
        it.status == AppointmentStatus.PENDING || it.status == AppointmentStatus.RESCHEDULED
    }
    val completed = active.count { it.status == AppointmentStatus.COMPLETED }
    val next = active
        .filter { it.date >= today && it.status != AppointmentStatus.COMPLETED }
        .minWithOrNull(compareBy({ it.date }, { it.timeSlot }))

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatTile("Today", todayCount, Modifier.weight(1f))
            StatTile("Waiting", waiting, Modifier.weight(1f))
            StatTile("Completed", completed, Modifier.weight(1f))
        }
        next?.let { appt ->
            MedFusionCard(contentPadding = Spacing.lg) {
                Text("Next consultation", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                Text("${appt.patientName.ifBlank { "Patient" }} • ${appt.date} at ${appt.timeSlot}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(appt.status.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    MedFusionCard(modifier = modifier, contentPadding = Spacing.md) {
        Text("$value", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HighPriorityCard(
    appointments: List<Appointment>,
    onOpenConsultation: (String) -> Unit,
) {
    MedFusionCard(contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, contentDescription = null,
                tint = MaterialTheme.semantic.riskRed)
            Spacer(Modifier.width(Spacing.sm))
            Text("High priority patients", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.semantic.riskRed)
        }
        appointments.forEach { appt ->
            Spacer(Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(appt.patientName.ifBlank { "Patient" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    Text("${appt.date} at ${appt.timeSlot}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.TextButton(onClick = { onOpenConsultation(appt.id) }) {
                    Text("Open")
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String?, modifier: Modifier = Modifier) {
    Text(
        text = if (name != null) "Welcome, Dr. $name" else "Welcome, Doctor",
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier,
    )
}
