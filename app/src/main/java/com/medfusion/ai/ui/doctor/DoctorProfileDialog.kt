package com.medfusion.ai.ui.doctor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.R
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Doctor
import com.medfusion.ai.domain.model.DoctorProfile
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loads one doctor's professional profile for the patient's pre-booking view. */
@HiltViewModel
class DoctorProfilePreviewViewModel @Inject constructor(
    private val profileRepository: DoctorProfileRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow<DoctorProfile?>(null)
    val profile: StateFlow<DoctorProfile?> = _profile.asStateFlow()

    fun load(doctorId: String) {
        viewModelScope.launch {
            _profile.value = (profileRepository.getProfile(doctorId) as? Resource.Success)?.data
        }
    }
}

/**
 * Patient-facing professional profile (Phase 6.5), shown before booking.
 * Falls back to the basic directory info when the doctor hasn't completed
 * their profile yet. Professional information only.
 */
@Composable
fun DoctorProfileDialog(
    doctor: Doctor,
    onDismiss: () -> Unit,
    viewModel: DoctorProfilePreviewViewModel = hiltViewModel(),
) {
    LaunchedEffect(doctor.id) { viewModel.load(doctor.id) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        MedFusionCard(contentPadding = Spacing.lg) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhoto(photoUri = profile?.photoUri.orEmpty(), sizeDp = 56)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            profile?.fullName?.ifBlank { null } ?: doctor.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            profile?.specialty?.ifBlank { null } ?: doctor.specialty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))

                val qualifications = profile?.qualifications?.ifBlank { null } ?: doctor.qualification
                val years = profile?.yearsExperience?.takeIf { it > 0 } ?: doctor.yearsExperience
                ProfileRow(stringResource(R.string.doctor_profile_qualifications), qualifications)
                ProfileRow(
                    stringResource(R.string.doctor_profile_experience),
                    if (years > 0) years.toString() else "",
                )
                ProfileRow(stringResource(R.string.doctor_profile_hospital), profile?.hospital.orEmpty())
                ProfileRow(stringResource(R.string.doctor_profile_languages), profile?.languagesSpoken.orEmpty())
                ProfileRow(stringResource(R.string.doctor_profile_availability), profile?.availability.orEmpty())
                ProfileRow(stringResource(R.string.doctor_profile_license), profile?.licenseNumber.orEmpty())

                profile?.biography?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                if (profile == null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        stringResource(R.string.doctor_profile_not_set),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
