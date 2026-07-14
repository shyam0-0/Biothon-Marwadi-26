package com.medfusion.ai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.medfusion.ai.R
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing

/**
 * App settings. Currently hosts the language picker (English / Hindi / Tamil),
 * which drives both the UI strings and the language of AI-generated content.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(LocaleManager.current()) }

    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "hi" to stringResource(R.string.language_hindi),
        "ta" to stringResource(R.string.language_tamil),
    )

    MedFusionScaffold(title = stringResource(R.string.settings), onBack = onBack) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Sizes.screenPadding, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.language_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))

            MedFusionCard(contentPadding = Spacing.xs) {
                languages.forEach { (tag, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = tag
                                // Recreates the activity to apply the new locale.
                                LocaleManager.set(tag)
                            }
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == tag, onClick = {
                            selected = tag
                            LocaleManager.set(tag)
                        })
                        Spacer(Modifier.width(Spacing.sm))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
