package com.medfusion.ai.ui.doctor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.viewmodel.DirectoryPatient
import com.medfusion.ai.viewmodel.DoctorPatientsViewModel

/**
 * Patient directory (Phase 6): the doctor's patients independent of any single
 * appointment. Search by name; selecting a patient opens their read-only record.
 */
@Composable
fun DoctorPatientsScreen(
    onOpenPatient: (patientId: String, patientName: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorPatientsViewModel = hiltViewModel(),
) {
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Patients", onBack = onBack) { padding ->
        Column(modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Sizes.screenPadding, vertical = Spacing.sm),
                placeholder = { Text("Search patients…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            if (patients.isEmpty()) {
                EmptyView(
                    title = if (query.isBlank()) "No patients yet" else "No matching patients",
                    subtitle = if (query.isBlank())
                        "Patients appear here once they book an appointment with you."
                    else "Try a different name.",
                    icon = Icons.Outlined.PersonSearch,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = Sizes.screenPadding,
                        end = Sizes.screenPadding,
                        bottom = Spacing.xl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(patients, key = { it.patientId }) { patient ->
                        PatientRow(patient) { onOpenPatient(patient.patientId, patient.patientName) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientRow(patient: DirectoryPatient, onClick: () -> Unit) {
    MedFusionCard(modifier = Modifier.clickable(onClick = onClick), contentPadding = Spacing.lg) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(patient.patientName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    "Last visit ${patient.lastVisitDate} • ${patient.visitCount} appointment(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (patient.highPriority) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "High priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.semantic.riskRed,
                )
            }
        }
    }
}
