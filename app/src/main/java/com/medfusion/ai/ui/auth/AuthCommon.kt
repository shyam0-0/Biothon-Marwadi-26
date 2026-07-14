package com.medfusion.ai.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.AuthEvent
import com.medfusion.ai.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Collects one-shot [AuthEvent]s safely across lifecycle, invoking [onAuthenticated]
 * exactly once per successful sign-in. Shared by Login and Register screens.
 */
@Composable
fun AuthEventEffect(viewModel: AuthViewModel, onAuthenticated: (User) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.events
            .flowWithLifecycle(lifecycleOwner.lifecycle)
            .collectLatest { event ->
                when (event) {
                    is AuthEvent.Authenticated -> onAuthenticated(event.user)
                }
            }
    }
}

/** Inline error message for auth forms; animates in when an error is present. */
@Composable
fun AuthErrorText(error: AppError?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = error != null) {
        Text(
            text = error?.userMessage.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(top = Spacing.sm),
        )
    }
}
