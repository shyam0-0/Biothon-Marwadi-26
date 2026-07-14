package com.medfusion.ai.ui.theme

import androidx.compose.ui.unit.dp

/**
 * A single spacing scale so every screen shares consistent rhythm. Referenced
 * as `Spacing.md` etc. rather than sprinkling magic dp values through the UI.
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Standard touch/target and layout sizes for accessibility-compliant UI. */
object Sizes {
    val minTouchTarget = 48.dp
    val buttonHeight = 52.dp
    val iconSm = 18.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val screenPadding = 20.dp
}
