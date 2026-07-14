package com.medfusion.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.ui.theme.Spacing

/** Centered full-screen loading indicator with an optional message. */
@Composable
fun LoadingView(modifier: Modifier = Modifier, message: String? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            if (message != null) {
                Text(
                    message,
                    modifier = Modifier.padding(top = Spacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Generic centered message with an icon, used by empty & error states. */
@Composable
private fun MessageState(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (action != null) {
                Box(Modifier.padding(top = Spacing.sm)) { action() }
            }
        }
    }
}

@Composable
fun EmptyView(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
) = MessageState(icon, title, subtitle, modifier)

/**
 * Error state with a retry action. The icon reflects offline vs. general error,
 * and retry is only offered when the [AppError] is retryable.
 */
@Composable
fun ErrorView(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val icon = if (error is AppError.Network) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline
    MessageState(
        icon = icon,
        title = if (error is AppError.Network) "You're offline" else "Something went wrong",
        subtitle = error.userMessage,
        modifier = modifier,
        action = if (onRetry != null && error.isRetryable) {
            { SecondaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f)) }
        } else null,
    )
}

/**
 * Declaratively renders the correct view for a [UiState]. Every screen routes
 * its state through this so Loading/Empty/Error/Success handling is identical
 * app-wide, fulfilling the "every screen must support all states" requirement.
 */
@Composable
fun <T> StateContainer(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    loadingMessage: String? = null,
    emptyTitle: String = "Nothing here yet",
    emptySubtitle: String? = null,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        when (state) {
            is UiState.Idle, is UiState.Loading -> LoadingView(message = loadingMessage)
            is UiState.Empty -> EmptyView(title = emptyTitle, subtitle = emptySubtitle)
            is UiState.Error -> ErrorView(error = state.error, onRetry = onRetry)
            is UiState.Success -> content(state.data)
        }
    }
}
