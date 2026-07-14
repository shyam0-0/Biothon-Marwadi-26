package com.medfusion.ai.ui.triage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.UrgencyChip
import com.medfusion.ai.ui.theme.Spacing

/**
 * Presents the triage recommendation: which test to get, how urgent it is (color
 * coded), and the next step — upload results once the test is done.
 */
@Composable
fun TriageResultCard(
    case: Case,
    onUploadNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MedFusionCard(modifier = modifier, contentPadding = Spacing.lg) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recommended next test",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            UrgencyChip(level = case.urgencyLevel)
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(case.recommendedTest, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(Spacing.md))
        Text(
            guidanceFor(case.urgencyLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = "I have my results — Upload now",
            leadingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onUploadNow,
        )
    }
}

private fun guidanceFor(level: UrgencyLevel): String = when (level) {
    UrgencyLevel.RED ->
        "Please get this test done as soon as possible, then upload your results in the next step."
    UrgencyLevel.YELLOW ->
        "Please get this test done in the next few days, then upload your results in the next step."
    UrgencyLevel.GREEN ->
        "When convenient, get this test done and upload your results in the next step."
}
