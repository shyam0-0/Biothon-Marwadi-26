package com.medfusion.ai.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.MedFusionTextField
import com.medfusion.ai.ui.components.PasswordTextField
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.TextActionButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.AuthViewModel

/**
 * Registration for the given [role]. Creates the Firebase Auth account and the
 * matching Firestore user document, then routes into the app on success.
 */
@Composable
fun RegisterScreen(
    role: UserRole,
    onAuthenticated: (User) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AuthEventEffect(viewModel, onAuthenticated)

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val roleLabel = if (role == UserRole.PATIENT) "Patient" else "Doctor"

    MedFusionScaffold(title = "Create $roleLabel Account", onBack = onBack) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Set up your MedFusion account to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))

            MedFusionCard(contentPadding = Spacing.lg) {
                MedFusionTextField(
                    value = fullName,
                    onValueChange = { fullName = it; viewModel.consumeError() },
                    label = "Full name",
                    leadingIcon = Icons.Outlined.Person,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(Spacing.md))
                MedFusionTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.consumeError() },
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(Spacing.md))
                PasswordTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.consumeError() },
                    label = "Password",
                    imeAction = ImeAction.Next,
                )
                Spacer(Modifier.height(Spacing.md))
                PasswordTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; viewModel.consumeError() },
                    label = "Confirm password",
                    imeAction = ImeAction.Done,
                )

                AuthErrorText(uiState.error)

                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(
                    text = "Create account",
                    loading = uiState.isSubmitting,
                    onClick = {
                        viewModel.register(fullName, email, password, confirmPassword, role)
                    },
                )
            }

            Spacer(Modifier.height(Spacing.md))
            TextActionButton(
                text = "Already have an account? Log in",
                onClick = onNavigateToLogin,
            )
            Spacer(Modifier.height(Spacing.md))
        }
    }
}
