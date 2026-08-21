package com.example.service

import android.content.Context
import android.content.Intent

/**
 * Backward compatibility alias for BackgroundAudioService
 */
class MahamVoiceService : BackgroundAudioService() {
    companion object {
        const val NOTIFICATION_ID = BackgroundAudioService.NOTIFICATION_ID
        const val ACTION_STOP_SERVICE = BackgroundAudioService.ACTION_STOP_SERVICE
        const val ACTION_START_LISTENING = BackgroundAudioService.ACTION_START_LISTENING

        val isRunning: Boolean
            get() = BackgroundAudioService.isRunning

        fun start(context: Context) {
            BackgroundAudioService.start(context)
        }

        fun stop(context: Context) {
            BackgroundAudioService.stop(context)
        }
    }
}
