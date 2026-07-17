package com.medfusion.ai.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.R
import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.NotificationKind
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.NotificationsViewModel
import java.text.DateFormat
import java.util.Date

/** Localized title for a notification kind — text follows the app language. */
@Composable
fun notificationTitle(kind: NotificationKind): String = stringResource(
    when (kind) {
        NotificationKind.APPOINTMENT_BOOKED -> R.string.notif_appointment_booked
        NotificationKind.APPOINTMENT_ACCEPTED -> R.string.notif_appointment_accepted
        NotificationKind.APPOINTMENT_RESCHEDULED -> R.string.notif_appointment_rescheduled
        NotificationKind.APPOINTMENT_CANCELLED -> R.string.notif_appointment_cancelled
        NotificationKind.CONSULTATION_STARTING_SOON -> R.string.notif_consultation_soon
        NotificationKind.DOCTOR_JOINED_CALL -> R.string.notif_doctor_joined
        NotificationKind.PRESCRIPTION_READY -> R.string.notif_prescription_ready
        NotificationKind.CARE_PLAN_READY -> R.string.notif_care_plan_ready
        NotificationKind.CHECK_IN_REMINDER -> R.string.notif_checkin_reminder
        NotificationKind.FOLLOW_UP_REMINDER -> R.string.notif_follow_up_reminder
        NotificationKind.RECOVERY_UPDATE -> R.string.notif_recovery_update
        NotificationKind.DEVICE_CONNECTION_LOST -> R.string.notif_device_lost
        NotificationKind.EMERGENCY_RECOMMENDATION -> R.string.notif_emergency
        NotificationKind.NEW_APPOINTMENT_REQUEST -> R.string.notif_new_request
        NotificationKind.REPORT_UPLOADED -> R.string.notif_report_uploaded
        NotificationKind.ANALYSIS_COMPLETED -> R.string.notif_analysis_completed
        NotificationKind.CONSULTATION_REMINDER -> R.string.notif_consultation_reminder
        NotificationKind.FOLLOW_UP_BOOKED -> R.string.notif_follow_up_booked
    },
)

/**
 * Bell icon with a live unread badge, shown on both dashboards' top bars.
 * Selecting it opens the notification center.
 */
@Composable
fun NotificationBell(
    onOpen: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    IconButton(onClick = onOpen) {
        BadgedBox(
            badge = {
                if (unread > 0) {
                    Badge { Text(if (unread > 9) "9+" else unread.toString()) }
                }
            },
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications),
            )
        }
    }
}

/**
 * Smart Notification Center (Phase 6.5): one screen serving both portals.
 * Tapping a notification marks it read and navigates to its target screen.
 */
@Composable
fun NotificationCenterScreen(
    onOpenRoute: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    MedFusionScaffold(
        title = stringResource(R.string.notifications),
        onBack = onBack,
        actions = {
            if (notifications.any { !it.read }) {
                IconButton(onClick = viewModel::markAllRead) {
                    Icon(
                        Icons.Outlined.DoneAll,
                        contentDescription = stringResource(R.string.notifications_mark_all_read),
                    )
                }
            }
            if (notifications.isNotEmpty()) {
                IconButton(onClick = viewModel::clearAll) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.notifications_clear),
                    )
                }
            }
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            EmptyView(
                title = stringResource(R.string.notifications_empty_title),
                subtitle = stringResource(R.string.notifications_empty_subtitle),
                icon = Icons.Outlined.Notifications,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Sizes.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            viewModel.markRead(notification.id)
                            notification.route?.let(onOpenRoute)
                        },
                    )
                }
                item { Spacer(Modifier.height(Spacing.md)) }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: AppNotification, onClick: () -> Unit) {
    MedFusionCard(contentPadding = Spacing.md, modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!notification.read) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    notificationTitle(notification.kind),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.SemiBold,
                )
                notification.detail?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(notification.timestampMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
