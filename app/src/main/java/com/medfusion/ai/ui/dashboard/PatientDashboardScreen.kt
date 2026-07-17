package com.medfusion.ai.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.ui.components.ActionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.notifications.NotificationBell
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.PatientDashboardViewModel
import com.medfusion.ai.viewmodel.SessionViewModel

/**
 * Patient home. The primary journey starts here with a symptom check; other
 * entry points (appointments, care plan, vitals) surface as they're built.
 */
@Composable
fun PatientDashboardScreen(
    onStartTriage: () -> Unit,
    onOpenAppointments: () -> Unit,
    onOpenCarePlan: () -> Unit,
    onOpenVitals: () -> Unit,
    onOpenPassport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = hiltViewModel(),
    homeViewModel: PatientDashboardViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val home by homeViewModel.state.collectAsStateWithLifecycle()
    val firstName = user?.fullName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

    MedFusionScaffold(
        title = "MedFusion",
        actions = {
            NotificationBell(onOpen = onOpenNotifications)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
            IconButton(onClick = { viewModel.logout(); onLoggedOut() }) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Log out")
            }
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = if (firstName != null) "Hello, $firstName" else "Hello",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "How are you feeling today? Start a quick check and we'll guide you to the right care.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))

            home.nextAppointment?.let { appt ->
                ActionCard(
                    title = "Upcoming appointment",
                    subtitle = "${appt.doctorName} • ${appt.date} at ${appt.timeSlot} (${appt.status.label})",
                    icon = Icons.Outlined.EventAvailable,
                    onClick = onOpenAppointments,
                )
            }

            ActionCard(
                title = "Start a symptom check",
                subtitle = "Describe your symptoms and get a recommended test",
                icon = Icons.Outlined.MedicalServices,
                onClick = onStartTriage,
            )
            ActionCard(
                title = "Patient passport",
                subtitle = "Your health profile, AI history and medical timeline",
                icon = Icons.Outlined.Badge,
                onClick = onOpenPassport,
            )
            ActionCard(
                title = "My appointments",
                subtitle = "View bookings and join video consultations",
                icon = Icons.Outlined.EventNote,
                onClick = onOpenAppointments,
            )
            ActionCard(
                title = "My care plan",
                subtitle = when {
                    home.checkInDueToday -> "Daily check-in due today — log how you're feeling"
                    home.hasCarePlan -> "Today's check-in recorded • medication reminders"
                    else -> "Medication schedule and daily check-ins"
                },
                icon = Icons.Outlined.CalendarMonth,
                onClick = onOpenCarePlan,
            )
            ActionCard(
                title = "Vitals monitor",
                subtitle = "Track heart rate and get emergency support",
                icon = Icons.Outlined.Favorite,
                onClick = onOpenVitals,
            )
        }
    }
}
