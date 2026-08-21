package com.example.session

import android.app.Application
import android.content.Context
import com.example.ai.GeminiAiEngine
import com.example.audio.AudioStreamManager
import com.example.audio.MahamSpeechManager
import com.example.audio.MahamTtsManager
import com.example.audio.WakeWordManager
import com.example.data.db.ConversationEntity
import com.example.data.db.MahamDatabase
import com.example.data.models.AssistantState
import com.example.data.models.LanguageMode
import com.example.data.models.MessageSender
import com.example.data.models.ToolExecutionOutcome
import com.example.data.models.UserPreferences
import com.example.service.BackgroundAudioService
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MahamLiveSessionManager(
    private val application: Application,
    private val scope: CoroutineScope
) {

    private val db = MahamDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()

    val toolEngine = ToolExecutionEngine(application)
    val aiEngine = GeminiAiEngine(toolEngine)
    val ttsManager = MahamTtsManager(application, scope)
    val audioStreamManager = AudioStreamManager(application)

    private val _assistantState = MutableStateFlow(AssistantState.VOICE_OFF)
    val assistantState = _assistantState.asStateFlow()

    private val _currentTranscription = MutableStateFlow("")
    val currentTranscription = _currentTranscription.asStateFlow()

    private val _liveAudioAmplitude = MutableStateFlow(0f)
    val liveAudioAmplitude = _liveAudioAmplitude.asStateFlow()

    private val _lastToolOutcome = MutableStateFlow<ToolExecutionOutcome?>(null)
    val lastToolOutcome = _lastToolOutcome.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var speechManager: MahamSpeechManager? = null
    private var wakeWordManager: WakeWordManager? = null
    private var continuousListenJob: Job? = null

    private var userPreferences: UserPreferences = UserPreferences()

    init {
        initSpeechManager()
        initWakeWordManager()

        // Sync speech amplitude when Maham is speaking
        scope.launch {
            ttsManager.speechAmplitude.collect { amp ->
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _liveAudioAmplitude.value = amp
                }
            }
        }
    }

    fun setPreferences(prefs: UserPreferences) {
        userPreferences = prefs
        ttsManager.pitch = prefs.voicePitch
        ttsManager.speechRate = prefs.speechRate

        if (prefs.voiceModeActive && _assistantState.value == AssistantState.VOICE_OFF) {
            _assistantState.value = AssistantState.IDLE
        } else if (!prefs.voiceModeActive) {
            _assistantState.value = AssistantState.VOICE_OFF
        }

        if (prefs.backgroundServiceEnabled) {
            BackgroundAudioService.start(application)
        } else {
            BackgroundAudioService.stop(application)
        }
    }

    private fun initSpeechManager() {
        speechManager = MahamSpeechManager(
            context = application,
            scope = scope,
            onResult = { finalResult ->
                _currentTranscription.value = finalResult
                processUserInput(finalResult)
            },
            onPartialResult = { partial ->
                _currentTranscription.value = partial
            },
            onError = { err ->
                if (_assistantState.value != AssistantState.VOICE_OFF) {
                    _assistantState.value = AssistantState.IDLE
                }
                _errorMessage.value = err
            },
            onUserBargeIn = {
                handleUserBargeIn()
            }
        )
    }

    private fun initWakeWordManager() {
        wakeWordManager = WakeWordManager(
            context = application,
            scope = scope,
            onWakeWordDetected = { phrase ->
                if (userPreferences.wakeWordEnabled) {
                    triggerGreetingAndListen()
                }
            }
        )
    }

    /**
     * Toggles Voice Assistant Mode ON / OFF.
     * When turned ON: Plays greeting "جی، کیا حکم ہے؟" with female voice, then starts listening.
     */
    fun toggleVoiceMode(enabled: Boolean? = null) {
        val newState = enabled ?: !userPreferences.voiceModeActive
        userPreferences = userPreferences.copy(voiceModeActive = newState)

        if (newState) {
            _assistantState.value = AssistantState.IDLE
            ttsManager.soundCues.playActivationChime()
            triggerGreetingAndListen()
        } else {
            continuousListenJob?.cancel()
            ttsManager.stop()
            speechManager?.stopListening()
            audioStreamManager.abandonAudioFocus()
            _assistantState.value = AssistantState.VOICE_OFF
            _liveAudioAmplitude.value = 0f
        }
    }

    /**
     * Plays greeting in natural female voice, then starts listening.
     */
    fun triggerGreetingAndListen() {
        continuousListenJob?.cancel()
        ttsManager.stop()

        val greetingText = "جی، کیا حکم ہے؟"
        _currentTranscription.value = greetingText
        _assistantState.value = AssistantState.SPEAKING

        audioStreamManager.requestAudioFocus()
        ttsManager.speak(greetingText, languageCode = "ur") {
            if (userPreferences.voiceModeActive) {
                startListening()
            } else {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    fun startListening() {
        if (!userPreferences.voiceModeActive) {
            userPreferences = userPreferences.copy(voiceModeActive = true)
        }

        continuousListenJob?.cancel()
        ttsManager.stop()
        audioStreamManager.requestAudioFocus()
        ttsManager.soundCues.playListeningStartCue()

        _assistantState.value = AssistantState.LISTENING
        _currentTranscription.value = ""
        _errorMessage.value = null

        val locale = when (userPreferences.languageMode) {
            LanguageMode.URDU -> "ur-PK"
            LanguageMode.PASHTO -> "ps-AF"
            LanguageMode.ENGLISH -> "en-US"
            LanguageMode.AUTO -> "ur-PK"
        }
        speechManager?.setLanguage(locale)
        speechManager?.startListening()

        scope.launch {
            speechManager?.audioAmplitude?.collect { amp ->
                if (_assistantState.value == AssistantState.LISTENING) {
                    _liveAudioAmplitude.value = amp
                }
            }
        }
    }

    fun stopListening() {
        continuousListenJob?.cancel()
        speechManager?.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = if (userPreferences.voiceModeActive) AssistantState.IDLE else AssistantState.VOICE_OFF
            _liveAudioAmplitude.value = 0f
        }
    }

    fun handleUserBargeIn() {
        if (ttsManager.isSpeaking.value || _assistantState.value == AssistantState.SPEAKING) {
            ttsManager.stop()
            _assistantState.value = AssistantState.LISTENING
            _liveAudioAmplitude.value = 0f
        }
    }

    fun interruptSpeech() {
        ttsManager.stop()
        stopListening()
        audioStreamManager.abandonAudioFocus()
        _assistantState.value = if (userPreferences.voiceModeActive) AssistantState.IDLE else AssistantState.VOICE_OFF
        _liveAudioAmplitude.value = 0f
    }

    fun processUserInput(userInput: String) {
        val trimmed = userInput.trim()
        if (trimmed.isBlank()) {
            _assistantState.value = if (userPreferences.voiceModeActive) AssistantState.IDLE else AssistantState.VOICE_OFF
            return
        }

        stopListening()

        // Standalone wake word trigger
        val lower = trimmed.lowercase()
        if (lower == "maham" || lower == "ماہم" || lower == "hey maham" || lower == "سنو ماہم") {
            triggerGreetingAndListen()
            return
        }

        _assistantState.value = AssistantState.THINKING

        scope.launch(Dispatchers.IO) {
            // Save User message
            conversationDao.insertMessage(
                ConversationEntity(
                    sender = MessageSender.USER.name,
                    text = trimmed,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Process with Gemini AI / Local NLU
            val result = aiEngine.processUserSpeech(
                userText = trimmed,
                customApiKey = userPreferences.customApiKey
            )

            // Save Maham message
            conversationDao.insertMessage(
                ConversationEntity(
                    sender = MessageSender.MAHAM.name,
                    text = result.spokenResponse,
                    timestamp = System.currentTimeMillis(),
                    language = result.detectedLanguage,
                    toolExecuted = result.toolExecuted,
                    isToolSuccess = result.isToolSuccess
                )
            )

            if (result.toolExecuted != null) {
                _lastToolOutcome.value = ToolExecutionOutcome(
                    toolName = result.toolExecuted,
                    success = result.isToolSuccess ?: true,
                    userFeedback = result.spokenResponse
                )
            }

            scope.launch(Dispatchers.Main) {
                _assistantState.value = AssistantState.SPEAKING
                _currentTranscription.value = result.spokenResponse

                audioStreamManager.requestAudioFocus()
                ttsManager.speak(
                    text = result.spokenResponse,
                    languageCode = result.detectedLanguage
                ) {
                    _assistantState.value = if (userPreferences.voiceModeActive) AssistantState.IDLE else AssistantState.VOICE_OFF
                    _liveAudioAmplitude.value = 0f

                    // Continuous voice conversation loop
                    if (userPreferences.voiceModeActive && userPreferences.autoListenAfterReply) {
                        continuousListenJob?.cancel()
                        continuousListenJob = scope.launch {
                            delay(400)
                            if (userPreferences.voiceModeActive && _assistantState.value == AssistantState.IDLE) {
                                startListening()
                            }
                        }
                    }
                }
            }
        }
    }

    fun clearHistory() {
        scope.launch(Dispatchers.IO) {
            conversationDao.clearHistory()
            aiEngine.resetConversation()
            _lastToolOutcome.value = null
            _currentTranscription.value = ""
        }
    }

    fun shutdown() {
        continuousListenJob?.cancel()
        speechManager?.stopListening()
        wakeWordManager?.shutdown()
        ttsManager.shutdown()
        audioStreamManager.abandonAudioFocus()
    }
}
