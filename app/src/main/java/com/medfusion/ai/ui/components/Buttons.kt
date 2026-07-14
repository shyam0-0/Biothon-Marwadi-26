package com.medfusion.ai.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing

/**
 * The primary call-to-action. One consistent height, shape and loading behaviour
 * everywhere — when [loading] is true it shows a spinner and disables itself so
 * users can't double-submit.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.buttonHeight),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.iconSm),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Secondary, lower-emphasis action rendered as an outlined button. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.buttonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tertiary inline action, e.g. "Don't have an account? Sign up". */
@Composable
fun TextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
