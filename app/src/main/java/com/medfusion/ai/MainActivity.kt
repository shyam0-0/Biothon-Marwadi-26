package com.medfusion.ai

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.medfusion.ai.navigation.MedFusionNavHost
import com.medfusion.ai.ui.theme.MedFusionTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Sets up the Compose content, applies the MedFusion theme,
 * and mounts the navigation graph. All screens live inside [MedFusionNavHost].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MedFusionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    MedFusionNavHost()
                }
            }
        }
    }
}
