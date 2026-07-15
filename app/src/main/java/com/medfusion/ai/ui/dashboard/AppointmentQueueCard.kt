package com.medfusion.ai.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.ui.components.ConfidenceChip
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.components.UrgencyChip
import com.medfusion.ai.ui.components.UrgencyDot
import com.medfusion.ai.ui.theme.Spacing

/**
 * A single request in the doctor's queue. Collapsed it shows the urgency badge and
 * key details; expanded it reveals the AI pre-read (fusion summary + confidence)
 * and report thumbnails, with Accept / Reschedule / Decline actions.
 */
@Composable
fun AppointmentQueueCard(
    appointment: Appointment,
    expanded: Boolean,
    caseState: UiState<Case>?,
    onToggle: () -> Unit,
    onAccept: () -> Unit,
    onReschedule: () -> Unit,
    onDecline: () -> Unit,
    onOpenConsultation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canConsult = appointment.status == AppointmentStatus.ACCEPTED ||
        appointment.status == AppointmentStatus.RESCHEDULED ||
        appointment.status == AppointmentStatus.COMPLETED
    MedFusionCard(modifier = modifier.clickable(onClick = onToggle), contentPadding = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrgencyDot(level = appointment.urgencyLevel)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    appointment.patientName.ifBlank { "Patient" },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${appointment.date} • ${appointment.timeSlot} • ${appointment.status.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            UrgencyChip(level = appointment.urgencyLevel)
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(Spacing.md))
                if (appointment.message.isNotBlank()) {
                    Text("Patient note", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(appointment.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.md))
                }

                Text("AI pre-read", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(Spacing.xs))
                PreReadSection(caseState)

                Spacer(Modifier.height(Spacing.md))
                if (canConsult) {
                    PrimaryButton(
                        text = "Open Consultation",
                        leadingIcon = Icons.Outlined.Videocam,
                        onClick = onOpenConsultation,
                    )
                    if (appointment.status != AppointmentStatus.COMPLETED) {
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SecondaryButton(text = "Reschedule", onClick = onReschedule, modifier = Modifier.weight(1f))
                            SecondaryButton(text = "Decline", onClick = onDecline, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        PrimaryButton(text = "Accept", onClick = onAccept, modifier = Modifier.weight(1f))
                        SecondaryButton(text = "Reschedule", onClick = onReschedule, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryButton(text = "Decline", onClick = onDecline)
                }
            }
        }
    }
}

@Composable
private fun PreReadSection(caseState: UiState<Case>?) {
    when (caseState) {
        null, is UiState.Idle, is UiState.Loading ->
            Box(Modifier.fillMaxWidth().height(72.dp)) { LoadingView() }
        is UiState.Error ->
            Text(
                "Couldn't load the AI pre-read.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        is UiState.Empty ->
            Text("No analysis available.", style = MaterialTheme.typography.bodyMedium)
        is UiState.Success -> {
            val case = caseState.data
            val fusion = case.fusionResult
            Text(
                fusion?.findings ?: "Analysis pending for this patient.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (fusion != null) {
                Spacer(Modifier.height(Spacing.sm))
                ConfidenceChip(level = fusion.confidenceLevel)
            }
            Spacer(Modifier.height(Spacing.md))
            ReportThumbnails(xrayUrl = case.xrayUrl, labReportUrl = case.labReportUrl)
        }
    }
}

@Composable
private fun ReportThumbnails(xrayUrl: String?, labReportUrl: String?) {
    if (xrayUrl == null && labReportUrl == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        xrayUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = "X-ray thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        labReportUrl?.let {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Description, contentDescription = "Lab report",
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Lab report", style = MaterialTheme.typography.labelMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
