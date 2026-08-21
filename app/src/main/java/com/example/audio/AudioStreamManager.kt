package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioStreamManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val _isAudioFocused = MutableStateFlow(false)
    val isAudioFocused = _isAudioFocused.asStateFlow()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> _isAudioFocused.value = true
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> _isAudioFocused.value = false
        }
    }

    /**
     * Request audio focus for speech playback or voice listening
     */
    fun requestAudioFocus(): Boolean {
        if (audioManager == null) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            focusRequest = request
            val res = audioManager.requestAudioFocus(request)
            val granted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            _isAudioFocused.value = granted
            granted
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            val granted = res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            _isAudioFocused.value = granted
            granted
        }
    }

    /**
     * Abandon audio focus when idle
     */
    fun abandonAudioFocus() {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
        _isAudioFocused.value = false
    }

    /**
     * Get Current Media Volume Level (0.0 to 1.0)
     */
    fun getMediaVolumeRatio(): Float {
        if (audioManager == null) return 0.5f
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) current.toFloat() / max else 0.5f
    }
}
