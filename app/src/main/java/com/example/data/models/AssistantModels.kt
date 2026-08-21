package com.example.data.models

enum class AssistantState(val label: String, val urduLabel: String, val pashtoLabel: String) {
    IDLE("Idle", "حاضر / منتظر", "انتظار / چمتو"),
    LISTENING("Listening...", "سن رہی ہوں...", "اورم..."),
    THINKING("Thinking...", "سوچ رہی ہوں...", "فکر کوم..."),
    SPEAKING("Speaking...", "بول رہی ہوں...", "خبرې کوم..."),
    ERROR("Error", "مسئلہ پیش آیا", "ستونزه رامنځته شوه"),
    VOICE_OFF("Voice Off", "وائس آف ہے", "غږ بند دی")
}

enum class LanguageMode(val displayName: String, val code: String, val locale: String) {
    AUTO("خودکار (Auto-Detect)", "auto", "ur-PK"),
    URDU("اردو (Urdu)", "ur", "ur-PK"),
    PASHTO("پښتو (Pashto)", "ps", "ps-AF"),
    ENGLISH("English", "en", "en-US")
}

data class VoiceMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detectedLanguage: String = "ur",
    val toolName: String? = null,
    val isToolSuccess: Boolean? = null
)

enum class MessageSender {
    USER,
    MAHAM,
    SYSTEM
}

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String
)

data class ToolExecutionOutcome(
    val toolName: String,
    val success: Boolean,
    val userFeedback: String,
    val details: String? = null
)

data class UserPreferences(
    val languageMode: LanguageMode = LanguageMode.AUTO,
    val voiceModeActive: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val backgroundServiceEnabled: Boolean = false,
    val voicePitch: Float = 1.20f,     // Naturally tuned warm female tone
    val speechRate: Float = 1.0f,
    val customApiKey: String = "",
    val autoListenAfterReply: Boolean = true,
    val isFirstLaunch: Boolean = true
)
