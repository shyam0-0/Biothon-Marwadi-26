package com.medfusion.ai.ui.analysis

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.ui.components.BrandLogo
import com.medfusion.ai.ui.components.InlineErrorCard
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing
import com.medfusion.ai.viewmodel.AnalysisViewModel

/**
 * Transient screen shown while the fusion model runs. On success it hands off to
 * the explainable result screen; on failure it offers a retry.
 */
@Composable
fun AnalysisScreen(
    onResultReady: (caseId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    (state as? UiState.Success)?.let { success ->
        androidx.compose.runtime.LaunchedEffect(success.data.caseId) {
            onResultReady(success.data.caseId)
        }
    }

    MedFusionScaffold(title = "Analysing", onBack = onBack) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Sizes.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is UiState.Error -> InlineErrorCard(error = s.error, onRetry = viewModel::analyze)
                else -> AnalysingContent()
            }
        }
    }
}

@Composable
private fun AnalysingContent() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            BrandLogo(size = 72.dp, modifier = Modifier.alpha(pulse))
        }
        Spacer(Modifier.height(Spacing.xl))
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            "Analysing your symptoms and reports",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Our AI is combining your inputs to generate a clear, explainable insight. This only takes a moment.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
