package com.medfusion.ai.ui.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.theme.Sizes

@Composable
fun VideoCallScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    MedFusionScaffold(
        title = "Video Consultation",
        onBack = onBack
    ) { padding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Sizes.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            EmptyView(
                title = "Video Consultation",
                subtitle = "This feature is temporarily disabled in the prototype build.",
                icon = Icons.Outlined.Videocam
            )

            PrimaryButton(
                text = "Go Back",
                onClick = onBack
            )
        }
    }
}