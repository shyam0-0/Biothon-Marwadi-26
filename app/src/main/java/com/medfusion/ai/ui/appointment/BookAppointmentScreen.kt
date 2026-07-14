package com.medfusion.ai.ui.appointment

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.ErrorView
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.BookAppointmentViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Lets a patient pick a date, choose an available slot (from doctor_availability),
 * add a message, and book. The case's urgency is attached automatically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    onBooked: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookAppointmentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.bookedEvents
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest { onBooked() }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = viewModel.todayMillis(),
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(viewModel::onDateSelected)
                    showDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    MedFusionScaffold(title = "Book Appointment", onBack = onBack) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Choose a date", style = MaterialTheme.typography.titleMedium)
            DateSelectorRow(
                dateIso = uiState.date,
                onClick = { showDatePicker = true },
            )

            if (uiState.date != null) {
                Text("Available time slots", style = MaterialTheme.typography.titleMedium)
                SlotSection(
                    availability = uiState.availability,
                    selected = uiState.selectedSlot,
                    onSelect = viewModel::onSlotSelected,
                )
            }

            Text("Message to the doctor (optional)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.message,
                onValueChange = viewModel::onMessageChange,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                placeholder = { Text("Anything you'd like the doctor to know…") },
                shape = MaterialTheme.shapes.medium,
            )

            uiState.error?.let {
                Text(it.userMessage, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(
                text = "Confirm Booking",
                enabled = uiState.selectedSlot != null,
                loading = uiState.isBooking,
                onClick = viewModel::book,
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun DateSelectorRow(dateIso: String?, onClick: () -> Unit) {
    MedFusionCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                dateIso?.let { friendlyDate(it) } ?: "Tap to select a date",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotSection(
    availability: UiState<List<AvailabilitySlot>>,
    selected: AvailabilitySlot?,
    onSelect: (AvailabilitySlot) -> Unit,
) {
    when (availability) {
        is UiState.Idle, is UiState.Loading ->
            Box(Modifier.fillMaxWidth().height(80.dp)) { LoadingView() }
        is UiState.Empty ->
            EmptyView(
                title = "No slots available",
                subtitle = "Please try a different date.",
                modifier = Modifier.height(140.dp),
            )
        is UiState.Error ->
            ErrorView(error = availability.error, modifier = Modifier.height(160.dp))
        is UiState.Success -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                availability.data.forEach { slot ->
                    val isSelected = slot == selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(slot) },
                        label = { Text(slot.timeSlot) },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }
        }
    }
}

private fun friendlyDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
} catch (_: Exception) {
    iso
}
