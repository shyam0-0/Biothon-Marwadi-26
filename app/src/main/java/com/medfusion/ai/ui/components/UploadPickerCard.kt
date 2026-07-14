package com.medfusion.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medfusion.ai.ui.theme.semantic
import com.medfusion.ai.ui.theme.Spacing

/**
 * Dashed upload target that flips to a filled "selected" state once a file is
 * chosen. Reused for both the X-ray and lab-report pickers so uploads look
 * identical everywhere.
 */
@Composable
fun UploadPickerCard(
    title: String,
    hint: String,
    icon: ImageVector,
    selectedName: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selected = selectedName != null
    val container = if (selected) MaterialTheme.semantic.riskGreenContainer
    else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .then(
                if (!selected) Modifier.border(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.large,
                ) else Modifier
            )
            .clickable(enabled = enabled && !selected, onClick = onPick)
            .padding(Spacing.md),
    ) {
        if (selected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.semantic.riskGreen,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge)
                    Text(
                        selectedName!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (enabled) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove $title")
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Outlined.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
