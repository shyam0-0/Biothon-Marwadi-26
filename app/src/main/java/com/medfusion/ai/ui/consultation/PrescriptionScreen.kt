package com.medfusion.ai.ui.consultation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.R
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.ui.components.InfoBanner
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.StateContainer
import com.medfusion.ai.viewmodel.PrescriptionViewModel

/** Patient-facing digital prescription from a completed consultation (Phase 2). */
@Composable
fun PrescriptionScreen(
    onBack: () -> Unit,
    onOpenCarePlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrescriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MedFusionScaffold(title = "Prescription", onBack = onBack) { padding ->
        StateContainer(
            state = state,
            modifier = Modifier.padding(padding),
            emptyTitle = "No prescription yet",
            emptySubtitle = "Your prescription will appear here after the doctor completes the consultation.",
            onRetry = viewModel::load,
        ) { prescription ->
            Content(prescription, onOpenCarePlan, modifier)
        }
    }
}

@Composable
private fun Content(
    prescription: Prescription,
    onOpenCarePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MedFusionCard {
            Text("Diagnosis", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(prescription.diagnosis.ifBlank { "—" }, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Prescribed by ${prescription.doctorName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (prescription.medications.isNotEmpty()) {
            Text("Medications", style = MaterialTheme.typography.titleMedium)
            prescription.medications.forEach { med ->
                MedFusionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Medication, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(med.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${med.dosage} • ${med.timing}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (prescription.advice.isNotBlank()) {
            MedFusionCard {
                Text("Advice", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(prescription.advice, style = MaterialTheme.typography.bodyMedium)
            }
        }

        prescription.followUpDate?.let {
            MedFusionCard {
                Text("Next follow-up", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Phase 4: the prescription flows straight into recovery tracking.
        PrimaryButton(text = "View my care plan", onClick = onOpenCarePlan)

        InfoBanner(text = stringResource(R.string.ai_disclaimer))
        Spacer(Modifier.height(16.dp))
    }
}
