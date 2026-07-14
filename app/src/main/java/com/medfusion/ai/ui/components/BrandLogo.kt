package com.medfusion.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The MedFusion mark — a heartbeat glyph on a rounded primary tile. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, size: Dp = 64.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.MonitorHeart,
            contentDescription = "MedFusion AI",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
