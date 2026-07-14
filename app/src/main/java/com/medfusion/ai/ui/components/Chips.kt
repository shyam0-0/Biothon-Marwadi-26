package com.medfusion.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.medfusion.ai.domain.model.ConfidenceLevel
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.ui.theme.semantic

/** Foreground/background pair for a semantic pill. */
private data class ChipColors(val content: Color, val container: Color)

/**
 * Generic pill used for tags and statuses. Kept private-styled but public so any
 * screen can build a labeled chip with the app's shape and typography.
 */
@Composable
fun MedFusionChip(
    text: String,
    content: Color,
    container: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun urgencyColors(level: UrgencyLevel): ChipColors {
    val s = MaterialTheme.semantic
    return when (level) {
        UrgencyLevel.RED -> ChipColors(s.riskRed, s.riskRedContainer)
        UrgencyLevel.YELLOW -> ChipColors(s.riskYellow, s.riskYellowContainer)
        UrgencyLevel.GREEN -> ChipColors(s.riskGreen, s.riskGreenContainer)
    }
}

/** Color-coded urgency chip (green/yellow/red) used in triage results & queues. */
@Composable
fun UrgencyChip(level: UrgencyLevel, modifier: Modifier = Modifier) {
    val c = urgencyColors(level)
    MedFusionChip(text = level.label, content = c.content, container = c.container, modifier = modifier)
}

/** Small round color dot for compact list rows (doctor queue). */
@Composable
fun UrgencyDot(level: UrgencyLevel, modifier: Modifier = Modifier) {
    val c = urgencyColors(level)
    Spacer(
        modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(c.content)
    )
}

/** Confidence chip (Low/Moderate/High) for the explainable result screen. */
@Composable
fun ConfidenceChip(level: ConfidenceLevel, modifier: Modifier = Modifier) {
    val s = MaterialTheme.semantic
    val c = when (level) {
        ConfidenceLevel.HIGH -> ChipColors(s.riskGreen, s.riskGreenContainer)
        ConfidenceLevel.MODERATE -> ChipColors(s.riskYellow, s.riskYellowContainer)
        ConfidenceLevel.LOW -> ChipColors(s.riskRed, s.riskRedContainer)
    }
    MedFusionChip(
        text = "${level.label} confidence",
        content = c.content,
        container = c.container,
        modifier = modifier,
    )
}
