package com.medfusion.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medfusion.ai.ui.theme.Spacing

/**
 * The base surface for the whole app: a rounded, subtly-elevated white card with
 * a hairline outline. Every card-based component builds on this so elevation,
 * radius and padding stay identical everywhere.
 */
@Composable
fun MedFusionCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = Spacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}
