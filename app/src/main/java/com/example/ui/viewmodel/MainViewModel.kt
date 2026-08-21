package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MahamTtsManager
import com.example.data.db.ConversationEntity
import com.example.data.db.MahamDatabase
import com.example.data.models.AssistantState
import com.example.data.models.LanguageMode
import com.example.data.models.ToolExecutionOutcome
import com.example.data.models.UserPreferences
import com.example.device.DeviceControlManager
import com.example.session.MahamLiveSessionManager
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MahamDatabase.getDatabase(application)
    private val conversationDao = db.conversationDao()

    val conversationHistory: StateFlow<List<ConversationEntity>> = conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val liveSessionManager = MahamLiveSessionManager(application, viewModelScope)

    val toolEngine: ToolExecutionEngine = liveSessionManager.toolEngine
    val deviceControlManager: DeviceControlManager = liveSessionManager.toolEngine.deviceManager
    val ttsManager: MahamTtsManager = liveSessionManager.ttsManager

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    val assistantState: StateFlow<AssistantState> = liveSessionManager.assistantState
    val currentTranscription: StateFlow<String> = liveSessionManager.currentTranscription
    val liveAudioAmplitude: StateFlow<Float> = liveSessionManager.liveAudioAmplitude
    val lastToolOutcome: StateFlow<ToolExecutionOutcome?> = liveSessionManager.lastToolOutcome
    val errorMessage: StateFlow<String?> = liveSessionManager.errorMessage

    init {
        liveSessionManager.setPreferences(_userPreferences.value)
    }

    /**
     * Toggles Voice Assistant Mode ON / OFF.
     */
    fun toggleVoiceMode(enabled: Boolean? = null) {
        val newState = enabled ?: !_userPreferences.value.voiceModeActive
        val updatedPrefs = _userPreferences.value.copy(voiceModeActive = newState)
        updatePreferences(updatedPrefs)
        liveSessionManager.toggleVoiceMode(newState)
    }

    fun triggerGreetingAndListen() {
        liveSessionManager.triggerGreetingAndListen()
    }

    fun startListening() {
        if (!_userPreferences.value.voiceModeActive) {
            val updatedPrefs = _userPreferences.value.copy(voiceModeActive = true)
            updatePreferences(updatedPrefs)
        }
        liveSessionManager.startListening()
    }

    fun stopListening() {
        liveSessionManager.stopListening()
    }

    fun interruptSpeech() {
        liveSessionManager.interruptSpeech()
    }

    fun processUserInput(userInput: String) {
        liveSessionManager.processUserInput(userInput)
    }

    fun clearHistory() {
        liveSessionManager.clearHistory()
    }

    fun increaseVolume(steps: Int = 1): ToolExecutionOutcome {
        return deviceControlManager.increaseVolume(steps)
    }

    fun decreaseVolume(steps: Int = 1): ToolExecutionOutcome {
        return deviceControlManager.decreaseVolume(steps)
    }

    fun setVolumePercent(percent: Int): ToolExecutionOutcome {
        return deviceControlManager.setVolumePercent(percent)
    }

    fun maxVolume(): ToolExecutionOutcome {
        return deviceControlManager.maxVolume()
    }

    fun muteVolume(): ToolExecutionOutcome {
        return deviceControlManager.muteVolume()
    }

    fun toggleFlashlight(enable: Boolean): ToolExecutionOutcome {
        return deviceControlManager.toggleFlashlight(enable)
    }

    fun getVolumeStatus(): ToolExecutionOutcome {
        return deviceControlManager.getVolumeStatus()
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        _userPreferences.value = newPrefs
        liveSessionManager.setPreferences(newPrefs)
        savePreferences(newPrefs)
    }

    private fun loadPreferences(): UserPreferences {
        val prefs = getApplication<Application>().getSharedPreferences("maham_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("lang_mode", "auto") ?: "auto"
        val langMode = LanguageMode.entries.find { it.code == langCode } ?: LanguageMode.AUTO

        return UserPreferences(
            languageMode = langMode,
            voiceModeActive = prefs.getBoolean("voice_active", true),
            wakeWordEnabled = prefs.getBoolean("wake_word", true),
            backgroundServiceEnabled = prefs.getBoolean("bg_service", false),
            voicePitch = prefs.getFloat("voice_pitch", 1.20f),
            speechRate = prefs.getFloat("speech_rate", 1.0f),
            customApiKey = prefs.getString("api_key", "") ?: "",
            autoListenAfterReply = prefs.getBoolean("auto_listen", true),
            isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        )
    }

    private fun savePreferences(prefs: UserPreferences) {
        val sp = getApplication<Application>().getSharedPreferences("maham_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("lang_mode", prefs.languageMode.code)
            .putBoolean("voice_active", prefs.voiceModeActive)
            .putBoolean("wake_word", prefs.wakeWordEnabled)
            .putBoolean("bg_service", prefs.backgroundServiceEnabled)
            .putFloat("voice_pitch", prefs.voicePitch)
            .putFloat("speech_rate", prefs.speechRate)
            .putString("api_key", prefs.customApiKey)
            .putBoolean("auto_listen", prefs.autoListenAfterReply)
            .putBoolean("is_first_launch", prefs.isFirstLaunch)
            .apply()
    }

    fun markFirstLaunchComplete() {
        val current = _userPreferences.value.copy(isFirstLaunch = false)
        updatePreferences(current)
    }

    override fun onCleared() {
        super.onCleared()
        liveSessionManager.shutdown()
    }
}
