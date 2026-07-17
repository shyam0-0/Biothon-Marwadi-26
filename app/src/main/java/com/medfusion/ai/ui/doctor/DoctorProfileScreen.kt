package com.medfusion.ai.ui.doctor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.medfusion.ai.R
import com.medfusion.ai.ui.components.LoadingView
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.DoctorProfileViewModel

/**
 * Doctor Profile Setup (Phase 6.5): a lightweight professional profile the
 * doctor can complete and edit at any time. NOT authentication — purely the
 * public card a patient reviews before booking. Professional info only.
 */
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val validationMessage = stringResource(R.string.doctor_profile_validation)

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            // Keep read access across restarts so the photo keeps loading.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.update { it.copy(photoUri = uri.toString()) }
        }
    }

    MedFusionScaffold(title = stringResource(R.string.doctor_profile), onBack = onBack) { padding ->
        val profile = state.profile
        if (state.loading || profile == null) {
            LoadingView(
                modifier = Modifier.fillMaxSize().padding(padding),
                message = stringResource(R.string.loading_profile),
            )
            return@MedFusionScaffold
        }
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                stringResource(R.string.doctor_profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MedFusionCard(contentPadding = Spacing.lg) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfilePhoto(photoUri = profile.photoUri, sizeDp = 72)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            stringResource(R.string.doctor_profile_photo),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        SecondaryButton(
                            text = stringResource(R.string.doctor_profile_choose_photo),
                            leadingIcon = Icons.Outlined.AddAPhoto,
                            onClick = { photoPicker.launch(arrayOf("image/*")) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = profile.fullName,
                onValueChange = { v -> viewModel.update { it.copy(fullName = v) } },
                label = { Text(stringResource(R.string.doctor_profile_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.specialty,
                onValueChange = { v -> viewModel.update { it.copy(specialty = v) } },
                label = { Text(stringResource(R.string.doctor_profile_specialty)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.qualifications,
                onValueChange = { v -> viewModel.update { it.copy(qualifications = v) } },
                label = { Text(stringResource(R.string.doctor_profile_qualifications)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = if (profile.yearsExperience == 0) "" else profile.yearsExperience.toString(),
                onValueChange = { v ->
                    val years = v.filter { c -> c.isDigit() }.take(2).toIntOrNull() ?: 0
                    viewModel.update { it.copy(yearsExperience = years) }
                },
                label = { Text(stringResource(R.string.doctor_profile_experience)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.hospital,
                onValueChange = { v -> viewModel.update { it.copy(hospital = v) } },
                label = { Text(stringResource(R.string.doctor_profile_hospital)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.languagesSpoken,
                onValueChange = { v -> viewModel.update { it.copy(languagesSpoken = v) } },
                label = { Text(stringResource(R.string.doctor_profile_languages)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.biography,
                onValueChange = { v -> viewModel.update { it.copy(biography = v) } },
                label = { Text(stringResource(R.string.doctor_profile_bio)) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            OutlinedTextField(
                value = profile.availability,
                onValueChange = { v -> viewModel.update { it.copy(availability = v) } },
                label = { Text(stringResource(R.string.doctor_profile_availability)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = profile.licenseNumber,
                onValueChange = { v -> viewModel.update { it.copy(licenseNumber = v) } },
                label = { Text(stringResource(R.string.doctor_profile_license)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            state.error?.let {
                Text(
                    it.userMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.saved) {
                Text(
                    stringResource(R.string.doctor_profile_saved),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            PrimaryButton(
                text = stringResource(R.string.doctor_profile_save),
                loading = state.saving,
                onClick = { viewModel.save(validationMessage) },
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

/** Round professional photo with a person placeholder when unset. */
@Composable
fun ProfilePhoto(photoUri: String, sizeDp: Int, modifier: Modifier = Modifier) {
    if (photoUri.isNotBlank()) {
        AsyncImage(
            model = photoUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(sizeDp.dp).clip(CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((sizeDp / 2).dp),
            )
        }
    }
}
