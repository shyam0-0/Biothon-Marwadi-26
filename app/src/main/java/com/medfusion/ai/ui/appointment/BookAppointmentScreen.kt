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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.medfusion.ai.domain.model.Doctor
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.ErrorView
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.viewmodel.BookAppointmentViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Booking (Phase 2): the AI-recommended specialty seeds a doctor list; the patient
 * chooses a doctor, then a date and time, then confirms. The case's AI context is
 * carried through so the doctor's queue shows the pre-read.
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
        viewModel.bookedEvents.flowWithLifecycle(lifecycleOwner.lifecycle).collectLatest { onBooked() }
    }

    MedFusionScaffold(title = "Book Appointment", onBack = onBack) { padding ->
        if (uiState.selectedDoctor == null) {
            DoctorSelection(
                specialty = uiState.specialty,
                doctors = uiState.doctors,
                onSelect = viewModel::selectDoctor,
                modifier = modifier.padding(padding),
            )
        } else {
            Scheduling(
                state = uiState,
                onDateSelected = viewModel::onDateSelected,
                onSlotSelected = viewModel::onSlotSelected,
                onMessageChange = viewModel::onMessageChange,
                onChangeDoctor = viewModel::changeDoctor,
                onBook = viewModel::book,
                todayMillis = viewModel.todayMillis(),
                modifier = modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DoctorSelection(
    specialty: String,
    doctors: UiState<List<Doctor>>,
    onSelect: (Doctor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Choose a $specialty", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Recommended based on your AI symptom analysis.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (doctors) {
            is UiState.Loading, is UiState.Idle ->
                Box(Modifier.fillMaxWidth().height(160.dp)) { LoadingView() }
            is UiState.Empty ->
                EmptyView(title = "No doctors available", subtitle = "Please try again later.")
            is UiState.Error -> ErrorView(error = doctors.error)
            is UiState.Success -> doctors.data.forEach { DoctorCard(it, onSelect) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DoctorCard(doctor: Doctor, onSelect: (Doctor) -> Unit) {
    MedFusionCard(modifier = Modifier.clickable { onSelect(doctor) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(doctor.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(
                        doctor.specialty,
                        doctor.qualification.ifBlank { null },
                        if (doctor.yearsExperience > 0) "${doctor.yearsExperience} yrs exp" else null,
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (doctor.rating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${doctor.rating}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Scheduling(
    state: com.medfusion.ai.viewmodel.BookingUiState,
    onDateSelected: (Long) -> Unit,
    onSlotSelected: (AvailabilitySlot) -> Unit,
    onMessageChange: (String) -> Unit,
    onChangeDoctor: () -> Unit,
    onBook: () -> Unit,
    todayMillis: Long,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                    showDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = datePickerState) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MedFusionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(state.selectedDoctor?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(state.specialty, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onChangeDoctor) { Text("Change") }
            }
        }

        Text("Choose a date", style = MaterialTheme.typography.titleMedium)
        MedFusionCard(modifier = Modifier.clickable { showDatePicker = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(state.date?.let(::friendlyDate) ?: "Tap to select a date",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (state.date != null) {
            Text("Available time slots", style = MaterialTheme.typography.titleMedium)
            SlotSection(state.availability, state.selectedSlot, onSlotSelected)
        }

        Text("Message to the doctor (optional)", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("Anything you'd like the doctor to know…") },
            shape = MaterialTheme.shapes.medium,
        )

        state.error?.let {
            Text(it.userMessage, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            text = "Confirm Booking",
            enabled = state.selectedSlot != null,
            loading = state.isBooking,
            onClick = onBook,
        )
        Spacer(Modifier.height(16.dp))
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
            EmptyView(title = "No slots available", subtitle = "Please try a different date.",
                modifier = Modifier.height(140.dp))
        is UiState.Error -> ErrorView(error = availability.error, modifier = Modifier.height(160.dp))
        is UiState.Success -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            availability.data.forEach { slot ->
                FilterChip(
                    selected = slot == selected,
                    onClick = { onSelect(slot) },
                    label = { Text(slot.timeSlot) },
                )
            }
        }
    }
}

private fun friendlyDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
} catch (_: Exception) {
    iso
}
