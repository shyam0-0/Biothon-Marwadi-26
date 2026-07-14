package com.medfusion.ai.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.ui.components.BrandLogo
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.TextActionButton
import com.medfusion.ai.ui.components.MedFusionTextField
import com.medfusion.ai.ui.components.PasswordTextField
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.AuthViewModel

/**
 * Shared login screen for both audiences — [role] only changes the title and the
 * role guard passed to the repository, so patients and doctors share one screen.
 */
@Composable
fun LoginScreen(
    role: UserRole,
    onAuthenticated: (User) -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AuthEventEffect(viewModel, onAuthenticated)

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val title = if (role == UserRole.PATIENT) "Patient Sign In" else "Doctor Sign In"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        BrandLogo(size = 64.dp)
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Welcome back. Please sign in to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xl))

        MedFusionCard(contentPadding = Spacing.lg) {
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
                imeAction = ImeAction.Done,
            )

            AuthErrorText(uiState.error)

            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(
                text = "Log in",
                loading = uiState.isSubmitting,
                onClick = { viewModel.login(email, password, role) },
            )
        }

        Spacer(Modifier.height(Spacing.md))
        TextActionButton(
            text = "Don't have an account? Sign up",
            onClick = onNavigateToRegister,
        )
    }
}
