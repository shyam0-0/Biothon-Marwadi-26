package com.medfusion.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that live outside Material's ColorScheme but must still adapt
 * to light/dark. Exposed via a CompositionLocal so components read them the same
 * way they read [MaterialTheme.colorScheme] — keeping a single source of truth
 * for urgency/confidence coloring across the whole app.
 */
@Immutable
data class MedFusionSemanticColors(
    val riskGreen: Color,
    val riskGreenContainer: Color,
    val riskYellow: Color,
    val riskYellowContainer: Color,
    val riskRed: Color,
    val riskRedContainer: Color,
)

val LightSemanticColors = MedFusionSemanticColors(
    riskGreen = RiskGreen,
    riskGreenContainer = RiskGreenContainer,
    riskYellow = RiskYellow,
    riskYellowContainer = RiskYellowContainer,
    riskRed = RiskRed,
    riskRedContainer = RiskRedContainer,
)

val DarkSemanticColors = MedFusionSemanticColors(
    riskGreen = RiskGreen,
    riskGreenContainer = RiskGreenContainerDark,
    riskYellow = RiskYellow,
    riskYellowContainer = RiskYellowContainerDark,
    riskRed = RiskRed,
    riskRedContainer = RiskRedContainerDark,
)

val LocalSemanticColors = compositionLocalOf { LightSemanticColors }

/** Ergonomic accessor: `MaterialTheme.semantic.riskRed`. */
val MaterialTheme.semantic: MedFusionSemanticColors
    @Composable @ReadOnlyComposable
    get() = LocalSemanticColors.current
