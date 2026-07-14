package com.medfusion.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.ui.theme.Spacing

/**
 * Inline (non-full-screen) error surface for use inside scrollable content, with
 * an optional retry offered only when the error is retryable.
 */
@Composable
fun InlineErrorCard(
    error: AppError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    MedFusionCard(modifier = modifier, contentPadding = Spacing.md) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(error.userMessage, style = MaterialTheme.typography.bodyMedium)
        }
        if (onRetry != null && error.isRetryable) {
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(text = "Retry", onClick = onRetry)
        }
    }
}
