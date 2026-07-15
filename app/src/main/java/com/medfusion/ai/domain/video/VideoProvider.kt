package com.medfusion.ai.domain.video

/**
 * Modular video-consultation launcher. Business logic (creating/fetching the room
 * URL) lives in the appointment layer; this only *joins* a room. Swapping to
 * Jitsi / Agora / 100ms later means providing a different implementation — no
 * ViewModel or repository changes.
 */
interface VideoProvider {
    fun join(roomUrl: String)
}
