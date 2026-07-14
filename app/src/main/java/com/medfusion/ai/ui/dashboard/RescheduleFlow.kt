package com.medfusion.ai.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * Two-step reschedule for a doctor: pick a new date, then propose a new time slot.
 * Renders nothing when [appointmentId] is null. Kept self-contained so the doctor
 * dashboard just toggles the id to drive it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RescheduleFlow(
    appointmentId: String?,
    initialDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (appointmentId: String, epochMillis: Long, timeSlot: String) -> Unit,
) {
    if (appointmentId == null) return

    var pickedMillis by remember(appointmentId) { mutableStateOf<Long?>(null) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (pickedMillis == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    pickedMillis = datePickerState.selectedDateMillis ?: initialDateMillis
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Propose a time") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PROPOSED_SLOTS.forEach { slot ->
                        FilterChip(
                            selected = false,
                            onClick = { onConfirm(appointmentId, pickedMillis!!, slot) },
                            label = { Text(slot) },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )
    }
}

private val PROPOSED_SLOTS = listOf("09:00 AM", "10:30 AM", "12:00 PM", "02:00 PM", "03:30 PM", "05:00 PM")
