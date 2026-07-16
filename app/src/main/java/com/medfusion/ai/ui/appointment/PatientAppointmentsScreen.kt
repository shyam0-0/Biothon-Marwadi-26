package com.medfusion.ai.ui.appointment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.UrgencyChip
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.PatientAppointmentsViewModel

/** A patient's appointments — join a call (accepted) or view the prescription (completed). */
@Composable
fun PatientAppointmentsScreen(
    onViewPrescription: (appointmentId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientAppointmentsViewModel = hiltViewModel(),
) {
    val appointments by viewModel.appointments.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "My Appointments", onBack = onBack) { padding ->
        if (appointments.isEmpty()) {
            EmptyView(
                title = "No appointments yet",
                subtitle = "Book an appointment after your analysis to see it here.",
                icon = Icons.Outlined.EventBusy,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Sizes.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                error?.let {
                    item {
                        Text(
                            it.userMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                items(appointments, key = { it.id }) { appointment ->
                    PatientAppointmentCard(
                        appointment = appointment,
                        onJoinCall = { viewModel.joinCall(appointment.id) },
                        onViewPrescription = { onViewPrescription(appointment.id) },
                    )
                }
                item { Spacer(Modifier.height(Spacing.md)) }
            }
        }
    }
}

@Composable
private fun PatientAppointmentCard(
    appointment: Appointment,
    onJoinCall: () -> Unit,
    onViewPrescription: () -> Unit,
) {
    val canJoin = appointment.status == AppointmentStatus.ACCEPTED ||
        appointment.status == AppointmentStatus.RESCHEDULED
    val completed = appointment.status == AppointmentStatus.COMPLETED

    MedFusionCard(contentPadding = Spacing.md) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    appointment.doctorName.ifBlank { "Doctor" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${appointment.date} • ${appointment.timeSlot}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Status: ${appointment.status.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            UrgencyChip(level = appointment.urgencyLevel)
        }
        if (canJoin) {
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "Join Call",
                leadingIcon = Icons.Outlined.Videocam,
                onClick = onJoinCall,
            )
        }
        if (completed) {
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = "View Prescription",
                leadingIcon = Icons.Outlined.Description,
                onClick = onViewPrescription,
            )
        }
    }
}
