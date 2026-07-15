package com.medfusion.ai.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.DoctorDashboardViewModel
import com.medfusion.ai.viewmodel.SessionViewModel

/**
 * Doctor home: a live, urgency-sorted queue of patient requests. Each card expands
 * to reveal the AI pre-read and report thumbnails, with Accept/Reschedule/Decline.
 */
@Composable
fun DoctorDashboardScreen(
    onLoggedOut: () -> Unit,
    onOpenConsultation: (appointmentId: String) -> Unit,
    onOpenSettings: () -> Unit,
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
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
            IconButton(onClick = { sessionViewModel.logout(); onLoggedOut() }) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out")
            }
        },
    ) { padding ->
        if (queue.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Greeting(name, Modifier.padding(horizontal = Sizes.screenPadding, vertical = Spacing.md))
                EmptyView(
                    title = "No patient requests yet",
                    subtitle = "Incoming appointments appear here, most urgent first.",
                    icon = Icons.Outlined.Inbox,
                )
            }
        } else {
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

@Composable
private fun Greeting(name: String?, modifier: Modifier = Modifier) {
    Text(
        text = if (name != null) "Welcome, Dr. $name" else "Welcome, Doctor",
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier,
    )
}
