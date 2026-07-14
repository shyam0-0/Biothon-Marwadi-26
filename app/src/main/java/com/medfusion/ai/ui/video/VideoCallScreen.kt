package com.medfusion.ai.ui.video

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.ui.components.EmptyView
import com.medfusion.ai.ui.components.MedFusionScaffold
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.StateContainer
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing

/**
 * Hosts a video consultation. Requests camera + microphone, then loads the
 * appointment's room (a Daily.co prebuilt room URL) in a hardware-accelerated
 * WebView that grants media access to the call.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoCallScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VideoCallViewModel = hiltViewModel(),
) {
    val roomState by viewModel.roomUrl.collectAsStateWithLifecycle()
    val permissions = rememberMultiplePermissionsState(
        listOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
    )

    MedFusionScaffold(title = "Video Consultation", onBack = onBack) { padding ->
        if (!permissions.allPermissionsGranted) {
            PermissionRequest(
                modifier = Modifier.padding(padding),
                onGrant = { permissions.launchMultiplePermissionRequest() },
            )
            return@MedFusionScaffold
        }

        StateContainer(
            state = roomState,
            modifier = Modifier.padding(padding),
            loadingMessage = "Setting up your secure room…",
            onRetry = viewModel::joinRoom,
        ) { url ->
            CallWebView(url = url, modifier = modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PermissionRequest(modifier: Modifier = Modifier, onGrant: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Sizes.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyView(
            title = "Camera & microphone needed",
            subtitle = "We need access to your camera and microphone to start the consultation.",
            icon = Icons.Outlined.Videocam,
        )
        PrimaryButton(text = "Allow access", onClick = onGrant)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CallWebView(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                }
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    // Grant the in-call web content the camera/mic the user already approved.
                    override fun onPermissionRequest(request: PermissionRequest) {
                        request.grant(request.resources)
                    }
                }
                loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
    )
}
