package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class MahamTtsManager(
    private val context: Context,
    private val scope: CoroutineScope
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val soundCues = MahamSoundCueManager(context, scope)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking = _isSpeaking.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude = _speechAmplitude.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null
    private var amplitudeAnimationJob: Job? = null
    private var pendingSpeakRequest: Pair<String, String>? = null

    // Tuned for natural, pleasant female vocal pitch and clear pacing
    var pitch: Float = 1.20f
        set(value) {
            field = value
            tts?.setPitch(value)
        }

    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupTtsEngine()
            
            // Execute any pending speech requested during initialization
            pendingSpeakRequest?.let { (text, lang) ->
                pendingSpeakRequest = null
                speak(text, lang, onSpeechDoneCallback)
            }
        } else {
            isInitialized = false
        }
    }

    private fun setupTtsEngine() {
        tts?.apply {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            setAudioAttributes(audioAttributes)
            setPitch(pitch)
            setSpeechRate(speechRate)

            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startAmplitudeSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    stopSpeakingState()
                    scope.launch(Dispatchers.Main) {
                        onSpeechDoneCallback?.invoke()
                    }
                }

                override fun onError(utteranceId: String?) {
                    stopSpeakingState()
                    scope.launch(Dispatchers.Main) {
                        onSpeechDoneCallback?.invoke()
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    stopSpeakingState()
                    scope.launch(Dispatchers.Main) {
                        onSpeechDoneCallback?.invoke()
                    }
                }
            })
        }
    }

    /**
     * Selects the highest quality natural female voice available in the engine.
     */
    private fun applyBestFemaleVoice(targetLocale: Locale) {
        val ttsInstance = tts ?: return
        try {
            val availableVoices = ttsInstance.voices ?: emptySet()
            if (availableVoices.isNotEmpty()) {
                // Find female voice matching the language or locale
                val matchingLocaleVoices = availableVoices.filter { 
                    it.locale.language.equals(targetLocale.language, ignoreCase = true) 
                }

                val femaleVoice = matchingLocaleVoices.find { v ->
                    isFemaleVoice(v)
                } ?: matchingLocaleVoices.firstOrNull() 
                  ?: availableVoices.find { isFemaleVoice(it) }

                if (femaleVoice != null) {
                    ttsInstance.voice = femaleVoice
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isFemaleVoice(voice: Voice): Boolean {
        val name = voice.name.lowercase()
        val features = voice.features?.map { it.lowercase() } ?: emptyList()
        return name.contains("female") || 
               name.contains("woman") || 
               name.contains("girl") ||
               name.contains("sfg") || 
               name.contains("network") ||
               features.any { it.contains("female") }
    }

    fun speak(text: String, languageCode: String = "ur", onDone: (() -> Unit)? = null) {
        onSpeechDoneCallback = onDone

        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        soundCues.ensureAudibleVolume()

        if (!isInitialized || tts == null) {
            pendingSpeakRequest = Pair(text, languageCode)
            // Fallback timer if TTS engine takes too long to init
            scope.launch {
                delay(1500)
                if (!isSpeaking.value && pendingSpeakRequest != null) {
                    pendingSpeakRequest = null
                    onDone?.invoke()
                }
            }
            return
        }

        val locale = when (languageCode.lowercase()) {
            "ps", "pashto" -> Locale.forLanguageTag("ps-AF")
            "en", "english" -> Locale.US
            else -> Locale.forLanguageTag("ur-PK")
        }

        try {
            val langResult = tts?.setLanguage(locale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default / US with higher female pitch
                tts?.setLanguage(Locale.US)
                tts?.setPitch(pitch * 1.05f)
            } else {
                tts?.setPitch(pitch)
            }

            applyBestFemaleVoice(locale)
            tts?.setSpeechRate(speechRate)

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            }

            val utteranceId = "maham_voice_${System.currentTimeMillis()}"
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            
            if (result == TextToSpeech.ERROR) {
                stopSpeakingState()
                onDone?.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSpeakingState()
            onDone?.invoke()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSpeakingState()
    }

    private fun startAmplitudeSimulation() {
        amplitudeAnimationJob?.cancel()
        amplitudeAnimationJob = scope.launch(Dispatchers.Default) {
            while (isActive && _isSpeaking.value) {
                val base = 0.40f + Random.nextFloat() * 0.60f
                _speechAmplitude.value = base
                delay(55)
            }
            _speechAmplitude.value = 0f
        }
    }

    private fun stopSpeakingState() {
        _isSpeaking.value = false
        _speechAmplitude.value = 0f
        amplitudeAnimationJob?.cancel()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
