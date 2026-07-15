package com.medfusion.ai.data.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.medfusion.ai.domain.video.VideoProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [VideoProvider]: opens the room URL in an external app/browser (e.g. a
 * Jitsi/Daily web room). Requires no SDK and keeps the call surface swappable —
 * replacing this binding with a native SDK provider needs no other code changes.
 */
@Singleton
class BrowserVideoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoProvider {
    override fun join(roomUrl: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(roomUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
