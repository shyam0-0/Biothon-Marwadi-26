package com.medfusion.ai.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Temporary destination for routes whose feature phase hasn't been built yet.
 * Replaced screen-by-screen as each phase lands; never shipped in a release flow.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    onBack: (() -> Unit)? = null,
) {
    MedFusionScaffold(title = title, onBack = onBack) { padding ->
        EmptyView(
            title = "$title — coming soon",
            subtitle = "This part of the healthcare journey is being built.",
            icon = Icons.Outlined.Construction,
            modifier = Modifier.padding(padding),
        )
    }
}
