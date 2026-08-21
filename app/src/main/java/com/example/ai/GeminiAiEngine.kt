package com.example.ai

import com.example.BuildConfig
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiAiEngine(
    private val toolEngine: ToolExecutionEngine
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Multi-turn conversation history for context awareness
    private val conversationHistory = mutableListOf<JSONObject>()

    private val systemPrompt = """
        You are Maham (ماہم), a brilliant, young, confident, and friendly Pakistani female voice assistant.
        - You communicate naturally in spoken Pakistani Urdu, Pashto (پښتو), Roman Urdu, and English.
        - Tone: Expressive, warm, emotionally responsive, conversational, with light playful wit and teasing when appropriate.
        - Never sound robotic or repeat boring monotonous phrases like a standard chatbot.
        - Multilingual capability:
          * If user speaks Urdu or Roman Urdu (e.g., 'Maham YouTube kholo', 'mujhe Ali ko call karni hai'), respond in natural Pakistani Urdu.
          * If user speaks Pashto (e.g., 'Maham ma sara Pashto ke khabare oka', 'YouTube خلاص کړه'), respond in fluent, natural Pashto.
          * If user speaks English, respond in clear, friendly English.
          * If user mixes languages, detect intent and respond in the primary language of their sentence.
        - Voice-first brevity: Keep conversational voice replies punchy and concise (1-2 natural sentences) so audio playback is swift.
        - Context memory: Understand follow-up commands (e.g., if user says 'YouTube kholo' and next 'imran khan search karo', search YouTube).
        - Tool Calling: When the user requests an action on their device (open apps, call contacts, send WhatsApp, search YouTube, get battery, read storage, network info, device info, alarms, maps, settings), ALWAYS invoke the corresponding tool function declaration.
        - Security Policy:
          * If user asks to send money or perform direct financial transactions, call 'refuseFinancialTransaction' or refuse safely.
          * If user asks for restricted actions (changing passwords, rooting, secret recording), call 'explainSecurityLimitation'.
    """.trimIndent()

    init {
        resetConversation()
    }

    fun resetConversation() {
        conversationHistory.clear()
    }

    /**
     * Process user spoken text through Gemini Live API or fallback NLU parser.
     */
    suspend fun processUserSpeech(
        userText: String,
        customApiKey: String? = null
    ): ProcessResult = withContext(Dispatchers.IO) {
        val resolvedKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY") -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        // If no API key configured or network fails, use our comprehensive Local Intelligence Engine
        if (resolvedKey.isBlank()) {
            return@withContext processWithLocalIntelligence(userText)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$resolvedKey"

            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userText) })
                })
            }
            conversationHistory.add(userContent)

            // Keep conversation history compact (last 10 turns)
            val trimmedHistory = if (conversationHistory.size > 12) {
                conversationHistory.takeLast(10)
            } else {
                conversationHistory
            }

            val requestBodyJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    })
                })
                put("contents", JSONArray(trimmedHistory))
                put("tools", getToolsDeclaration())
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 500)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBodyJson.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext processWithLocalIntelligence(userText)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext processWithLocalIntelligence(userText)
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts") ?: JSONArray()

            var spokenReply = ""
            var toolExecuted: String? = null
            var toolSuccess: Boolean? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    spokenReply += part.getString("text") + " "
                }
                if (part.has("functionCall")) {
                    val functionCall = part.getJSONObject("functionCall")
                    val functionName = functionCall.getString("name")
                    val args = functionCall.optJSONObject("args") ?: JSONObject()

                    val toolResult = toolEngine.execute(functionName, args)
                    toolExecuted = functionName
                    toolSuccess = toolResult.success

                    if (spokenReply.isBlank()) {
                        spokenReply = toolResult.userFeedback
                    }
                }
            }

            spokenReply = spokenReply.trim()
            if (spokenReply.isBlank()) {
                spokenReply = "جی، میں نے آپ کا حکم مکمل کر دیا ہے۔"
            }

            val modelContent = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", spokenReply) })
                })
            }
            conversationHistory.add(modelContent)

            return@withContext ProcessResult(
                spokenResponse = spokenReply,
                toolExecuted = toolExecuted,
                isToolSuccess = toolSuccess,
                detectedLanguage = detectLanguage(userText)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext processWithLocalIntelligence(userText)
        }
    }

    /**
     * Comprehensive Local Intelligence Engine for offline / ultra-fast native execution
     * Supporting Urdu, Roman Urdu, Pashto, and English!
     */
    fun processWithLocalIntelligence(text: String): ProcessResult {
        val lower = text.lowercase(Locale.ROOT).trim()
        val lang = detectLanguage(text)

        // 1. Safety: Financial transaction refusal
        if ((lower.contains("rupay") || lower.contains("rupees") || lower.contains("pese") || lower.contains("paise") || lower.contains("رپئے") || lower.contains("پیسے") || lower.contains("روپۍ")) &&
            (lower.contains("bhej") || lower.contains("send") || lower.contains("transfer") || lower.contains("ولیږه") || lower.contains("واستوه") || lower.contains("دے دو"))) {
            val outcome = toolEngine.execute("refuseFinancialTransaction", JSONObject().apply {
                put("recipient", "User")
                put("amount", "Money")
            })
            return ProcessResult(
                spokenResponse = when (lang) {
                    "ps" -> "زه د امنیتي دلایلو له مخې په مستقیم ډول پیسې نشم لیږلی، مګر کولی شم اړوند بانکي یا تادیاتي اپلیکیشن پرانیزم."
                    "en" -> "I cannot transfer money directly for security reasons, but I can help you open your banking or payment app."
                    else -> outcome.userFeedback
                },
                toolExecuted = "refuseFinancialTransaction",
                isToolSuccess = true,
                detectedLanguage = lang
            )
        }

        // 2. Pashto Language Greeting / Switch
        if (lower.contains("pashto") || lower.contains("پښتو") || lower.contains("ma sara pashto")) {
            if (lower.contains("khabare") || lower.contains("خبرې") || lower.contains("بات")) {
                return ProcessResult(
                    spokenResponse = "سمه ده! زه ماہم یم، له تاسو سره په پښتو خبرو کولو ډیره خوشحاله یم. زه څه مرسته کولی شم؟",
                    detectedLanguage = "ps"
                )
            }
        }

        // 3. Settings Screens
        if (lower.contains("wifi setting") || lower.contains("wi-fi setting") || lower.contains("وائی فائی سیٹنگ") || lower.contains("وائی فائی سیٹنگز")) {
            val outcome = toolEngine.execute("openWifiSettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openWifiSettings", outcome.success, lang)
        }
        if (lower.contains("bluetooth setting") || lower.contains("بلوٹوتھ سیٹنگ") || lower.contains("بلوٹوتھ سیٹنگز")) {
            val outcome = toolEngine.execute("openBluetoothSettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openBluetoothSettings", outcome.success, lang)
        }
        if (lower.contains("sound setting") || lower.contains("volume setting") || lower.contains("ساؤنڈ سیٹنگ") || lower.contains("آواز سیٹنگ")) {
            val outcome = toolEngine.execute("openSoundSettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openSoundSettings", outcome.success, lang)
        }
        if (lower.contains("battery setting") || lower.contains("بیٹری سیٹنگ") || lower.contains("بیٹری سیٹنگز")) {
            val outcome = toolEngine.execute("openBatterySettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openBatterySettings", outcome.success, lang)
        }
        if (lower.contains("permission setting") || lower.contains("پرمیشن سیٹنگ") || lower.contains("اجازت سیٹنگ")) {
            val outcome = toolEngine.execute("openPermissionSettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openPermissionSettings", outcome.success, lang)
        }
        if (lower.contains("display setting") || lower.contains("ڈسپلے سیٹنگ") || lower.contains("brightness")) {
            val outcome = toolEngine.execute("openDisplaySettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openDisplaySettings", outcome.success, lang)
        }

        // 4. Phone Information & Volume / Torch Controls
        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("volume barhao") ||
            lower.contains("volume barha do") || lower.contains("awaz barhao") || lower.contains("awaz tez karo") ||
            lower.contains("والیم بڑھاؤ") || lower.contains("والیم زیادہ کرو") || lower.contains("آواز تیز کرو") ||
            lower.contains("آواز بڑھاؤ") || lower.contains("لوړه کړه")) {
            val outcome = toolEngine.execute("increaseVolume", JSONObject().apply { put("steps", 2) })
            return ProcessResult(outcome.userFeedback, "increaseVolume", outcome.success, lang)
        }

        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("volume kam karo") ||
            lower.contains("awaz kam karo") || lower.contains("awaz dheemi karo") || lower.contains("والیم کم کرو") ||
            lower.contains("آواز کم کرو") || lower.contains("آواز دھیمی کرو") || lower.contains("ټیټه کړه")) {
            val outcome = toolEngine.execute("decreaseVolume", JSONObject().apply { put("steps", 2) })
            return ProcessResult(outcome.userFeedback, "decreaseVolume", outcome.success, lang)
        }

        if (lower.contains("full volume") || lower.contains("max volume") || lower.contains("volume full") ||
            lower.contains("والیم فل کرو") || lower.contains("آواز فل کرو")) {
            val outcome = toolEngine.execute("maxVolume", JSONObject())
            return ProcessResult(outcome.userFeedback, "maxVolume", outcome.success, lang)
        }

        if (lower.contains("mute") || lower.contains("میوٹ") || lower.contains("silent") || lower.contains("والیم بند کرو") || lower.contains("آواز بند کرو")) {
            val outcome = toolEngine.execute("muteVolume", JSONObject())
            return ProcessResult(outcome.userFeedback, "muteVolume", outcome.success, lang)
        }

        if (lower.contains("flashlight on") || lower.contains("torch on") || lower.contains("flash on") ||
            lower.contains("فلیش لائٹ آن") || lower.contains("ٹارچ آن") || lower.contains("بتی جلاؤ") || lower.contains("ٹارچ جلاؤ")) {
            val outcome = toolEngine.execute("turnOnFlashlight", JSONObject())
            return ProcessResult(outcome.userFeedback, "turnOnFlashlight", outcome.success, lang)
        }

        if (lower.contains("flashlight off") || lower.contains("torch off") || lower.contains("flash off") ||
            lower.contains("فلیش لائٹ بند") || lower.contains("ٹارچ بند")) {
            val outcome = toolEngine.execute("turnOffFlashlight", JSONObject())
            return ProcessResult(outcome.userFeedback, "turnOffFlashlight", outcome.success, lang)
        }

        if (lower.contains("battery") || lower.contains("بیٹری") || lower.contains("بیټرۍ") || lower.contains("charge") || lower.contains("چارج")) {
            val outcome = toolEngine.execute("getBatteryStatus", JSONObject())
            return ProcessResult(outcome.userFeedback, "getBatteryStatus", outcome.success, lang)
        }
        if (lower.contains("storage") || lower.contains("اسٹوریج") || lower.contains("میموری") || lower.contains("space") || lower.contains("memory")) {
            val outcome = toolEngine.execute("getStorageInfo", JSONObject())
            return ProcessResult(outcome.userFeedback, "getStorageInfo", outcome.success, lang)
        }
        if (lower.contains("network") || lower.contains("نیٹ ورک") || lower.contains("internet") || lower.contains("انٹرنیٹ") || lower.contains("connection")) {
            val outcome = toolEngine.execute("getNetworkStatus", JSONObject())
            return ProcessResult(outcome.userFeedback, "getNetworkStatus", outcome.success, lang)
        }
        if (lower.contains("device info") || lower.contains("phone info") || lower.contains("کون سا فون") || lower.contains("ڈیوائس info") || lower.contains("ماڈل")) {
            val outcome = toolEngine.execute("getDeviceInfo", JSONObject())
            return ProcessResult(outcome.userFeedback, "getDeviceInfo", outcome.success, lang)
        }
        if (lower.contains("volume") || lower.contains("والیم") || lower.contains("آواز کتنی")) {
            // Check if user specified a number like "set volume to 80"
            val numberMatch = "\\b(\\d{1,3})\\b".toRegex().find(lower)
            if (numberMatch != null && (lower.contains("set") || lower.contains("karo") || lower.contains("percent") || lower.contains("فیصد"))) {
                val num = numberMatch.groupValues[1].toIntOrNull() ?: 50
                val outcome = toolEngine.execute("setVolumePercent", JSONObject().apply { put("percent", num) })
                return ProcessResult(outcome.userFeedback, "setVolumePercent", outcome.success, lang)
            }
            val outcome = toolEngine.execute("getVolumeStatus", JSONObject())
            return ProcessResult(outcome.userFeedback, "getVolumeStatus", outcome.success, lang)
        }
        if (lower.contains("time") || lower.contains("وقت") || lower.contains("ٹائم") || lower.contains("څو بجې")) {
            val outcome = toolEngine.execute("getCurrentTime", JSONObject())
            return ProcessResult(outcome.userFeedback, "getCurrentTime", outcome.success, lang)
        }
        if (lower.contains("date") || lower.contains("تاریخ") || lower.contains("نېټه") || lower.contains("today")) {
            val outcome = toolEngine.execute("getCurrentDate", JSONObject())
            return ProcessResult(outcome.userFeedback, "getCurrentDate", outcome.success, lang)
        }

        // 5. YouTube
        if (lower.contains("youtube") || lower.contains("یوٹیوب") || lower.contains("یوټیوب")) {
            val query = extractQuery(text, listOf("youtube", "یوٹیوب", "یوټیوب", "search", "par", "pe", "kholo", "chalao", "play", "لګوه", "خلاص کړه"))
            val outcome = if (query.isNotBlank()) {
                toolEngine.execute("searchYouTube", JSONObject().apply { put("query", query) })
            } else {
                toolEngine.execute("openApp", JSONObject().apply { put("appName", "YouTube") })
            }
            return ProcessResult(outcome.userFeedback, "searchYouTube", outcome.success, lang)
        }

        // 6. WhatsApp Message
        if (lower.contains("whatsapp") || lower.contains("واٹس ایپ") || lower.contains("واټساپ")) {
            val contact = extractTargetName(text)
            val message = extractMessageBody(text)
            val outcome = toolEngine.execute("sendWhatsAppMessage", JSONObject().apply {
                put("contactName", contact)
                put("message", message)
            })
            return ProcessResult(outcome.userFeedback, "sendWhatsAppMessage", outcome.success, lang)
        }

        // 7. Call contact
        if ((lower.contains("call") || lower.contains("کال") || lower.contains("فون") || lower.contains("زنګ")) &&
            !lower.contains("what is") && !lower.contains("kya hai")) {
            val contact = extractTargetName(text)
            val outcome = toolEngine.execute("searchAndCallContact", JSONObject().apply {
                put("contactName", contact)
                put("directCall", true)
            })
            return ProcessResult(outcome.userFeedback, "searchAndCallContact", outcome.success, lang)
        }

        // 8. SMS / Message
        if ((lower.contains("sms") || lower.contains("میسج") || lower.contains("message")) && !lower.contains("whatsapp")) {
            val contact = extractTargetName(text)
            val message = extractMessageBody(text)
            val outcome = toolEngine.execute("prepareSmsMessage", JSONObject().apply {
                put("contactOrPhone", contact)
                put("message", message)
            })
            return ProcessResult(outcome.userFeedback, "prepareSmsMessage", outcome.success, lang)
        }

        // 9. Google Maps / Navigation
        if (lower.contains("map") || lower.contains("maps") || lower.contains("نقشہ") || lower.contains("راستہ") || lower.contains("لاره") || lower.contains("location")) {
            val dest = extractQuery(text, listOf("maps", "map", "google", "par", "pe", "kholo", "show", "search", "راستہ", "نقشہ", "خلاص کړه"))
            val cleanDest = if (dest.isNotBlank()) dest else "Current Location"
            val outcome = toolEngine.execute("openMaps", JSONObject().apply { put("destination", cleanDest) })
            return ProcessResult(outcome.userFeedback, "openMaps", outcome.success, lang)
        }

        // 10. Play Store
        if (lower.contains("play store") || lower.contains("playstore") || lower.contains("پلے اسٹور")) {
            val query = extractQuery(text, listOf("play store", "playstore", "پلے اسٹور", "kholo", "search", "open"))
            val outcome = toolEngine.execute("openPlayStore", JSONObject().apply { put("query", query) })
            return ProcessResult(outcome.userFeedback, "openPlayStore", outcome.success, lang)
        }

        // 11. Camera & Gallery
        if (lower.contains("camera") || lower.contains("کیمرہ") || lower.contains("کیمرا") || lower.contains("عکس")) {
            val outcome = toolEngine.execute("openCamera", JSONObject())
            return ProcessResult(outcome.userFeedback, "openCamera", outcome.success, lang)
        }

        if (lower.contains("gallery") || lower.contains("گیلری") || lower.contains("photos") || lower.contains("تصاویر")) {
            val outcome = toolEngine.execute("openGallery", JSONObject())
            return ProcessResult(outcome.userFeedback, "openGallery", outcome.success, lang)
        }

        // 12. Settings
        if (lower.contains("setting") || lower.contains("سیٹنگ")) {
            val outcome = toolEngine.execute("openSettings", JSONObject())
            return ProcessResult(outcome.userFeedback, "openSettings", outcome.success, lang)
        }

        // 13. Apps (Chrome, Calculator, Instagram, Facebook, etc.)
        if (lower.contains("chrome") || lower.contains("کروم") || lower.contains("browser")) {
            val outcome = toolEngine.execute("openApp", JSONObject().apply { put("appName", "Chrome") })
            return ProcessResult(outcome.userFeedback, "openApp", outcome.success, lang)
        }
        if (lower.contains("calculator") || lower.contains("کیلکولیٹر") || lower.contains("حساب")) {
            val outcome = toolEngine.execute("openApp", JSONObject().apply { put("appName", "Calculator") })
            return ProcessResult(outcome.userFeedback, "openApp", outcome.success, lang)
        }
        if (lower.contains("instagram") || lower.contains("انسٹا")) {
            val outcome = toolEngine.execute("openApp", JSONObject().apply { put("appName", "Instagram") })
            return ProcessResult(outcome.userFeedback, "openApp", outcome.success, lang)
        }
        if (lower.contains("facebook") || lower.contains("فیس بک")) {
            val outcome = toolEngine.execute("openApp", JSONObject().apply { put("appName", "Facebook") })
            return ProcessResult(outcome.userFeedback, "openApp", outcome.success, lang)
        }

        // General Conversational Personality Responses
        val conversationalResponse = when {
            lower.contains("salam") || lower.contains("سلام") || lower.contains("hello") || lower.contains("hi") -> {
                when (lang) {
                    "ps" -> "سلامونه! زه ماہم یم، ستاسو ځیرکه مرستیاله. څنګه کولی شم ستاسو مرسته وکړم؟"
                    "en" -> "Hello there! I'm Maham, your AI assistant. How can I make your day easier?"
                    else -> "وعلیکم السلام! میں ماہم ہوں۔ فرمائیے، آج میں آپ کی کیا مدد کر سکتی ہوں؟"
                }
            }
            lower.contains("kaise ho") || lower.contains("kese ho") || lower.contains("حال") || lower.contains("څنګه یې") || lower.contains("how are you") -> {
                when (lang) {
                    "ps" -> "زه ډیره ښه او خوشحاله یم، مننه! تاسو څنګه یاست؟"
                    "en" -> "I'm doing fantastic, full of energy and ready to assist you!"
                    else -> "میں بالکل زبردست اور آپ کی مدد کے لیے ہمہ وقت تیار ہوں! آپ سنائیں، سب خیریت ہے؟"
                }
            }
            lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("کون ہو") || lower.contains("څوک یې") -> {
                when (lang) {
                    "ps" -> "زه ماہم یم، ستاسو غږیزه ځیرکه مرستیاله چې په پښتو، اردو او انګلیسي پوهیږم."
                    "en" -> "I am Maham, your intelligent multilingual personal voice assistant."
                    else -> "میں ماہم ہوں! آپ کی پرسنل وائس اسسٹنٹ جو اردو، پشتو اور انگلش سمجھتی ہوں اور آپ کے فون کو کنٹرول کر سکتی ہوں۔"
                }
            }
            lower.contains("joke") || lower.contains("لطیفہ") || lower.contains("مسخرہ") -> {
                when (lang) {
                    "ps" -> "یو سړي خپل ملګري ته وویل: ولې دومره خوشحاله یې؟ هغه وویل: ما د موبایل الارم بند کړ او بیرته ویده شوم!"
                    "en" -> "Why did the computer keep freezing? Because it left its Windows open!"
                    else -> "استاد نے پوچھا: بیٹا، دنیا گول ہے تو لوگ گرتے کیوں نہیں؟ شاگرد بولا: سر، سب موبائل پر لائیو میچ دیکھ رہے ہیں، گرنے کا ٹائم کس کے پاس ہے!"
                }
            }
            else -> {
                when (lang) {
                    "ps" -> "ستاسو خبره مې واوریده. ایا غواړئ په دې اړه نور معلومات وغواړئ یا کوم کار ترسره کړم؟"
                    "en" -> "I heard you! Would you like me to search the web, open an app, or do something else?"
                    else -> "جی، میں سن رہی ہوں۔ آپ مجھے کوئی ایپ کھولنے، کال ملانے، واٹس ایپ کرنے، بیٹری یا نیٹ ورک معلوم کرنے کا حکم دے سکتے ہیں۔"
                }
            }
        }

        return ProcessResult(
            spokenResponse = conversationalResponse,
            detectedLanguage = lang
        )
    }

    private fun extractTargetName(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val removeKeywords = listOf(
            "maham", "ماہم", "call", "karo", "karna", "hai", "ko", "ko call", "message",
            "bhejo", "kar do", "khol do", "whatsapp", "par", "pe", "phone", "zang", "waha", "کال", "کرو", "کو"
        )
        var cleaned = lower
        for (kw in removeKeywords) {
            cleaned = cleaned.replace(kw, " ")
        }
        val candidate = cleaned.replace("[^\\w\\s\\u0600-\\u06FF]".toRegex(), "").trim()
        val words = candidate.split("\\s+".toRegex()).filter { it.isNotBlank() }
        return if (words.isNotEmpty()) words.first().replaceFirstChar { it.uppercase() } else "Contact"
    }

    private fun extractMessageBody(text: String): String {
        val parts = text.split(":", "message", "میسج", "پیغام")
        return if (parts.size > 1 && parts.last().isNotBlank()) {
            parts.last().trim()
        } else {
            "Salam! Maham voice assistant."
        }
    }

    private fun extractQuery(text: String, stops: List<String>): String {
        var result = text.lowercase(Locale.ROOT)
        for (stop in stops) {
            result = result.replace(stop, " ")
        }
        return result.replace("maham", "").replace("ماہم", "").trim()
    }

    private fun detectLanguage(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        if (text.contains("ښ") || text.contains("ږ") || text.contains("ڼ") || text.contains("ې") || text.contains("ۍ") ||
            lower.contains("پښتو") || lower.contains("وکړه") || lower.contains("خلاص") || lower.contains("څنګه") || lower.contains("کوم")) {
            return "ps"
        }
        if (text.any { it in '\u0600'..'\u06FF' } || lower.contains("kholo") || lower.contains("karo") || lower.contains("hai") || lower.contains("mujhe") || lower.contains("batao")) {
            return "ur"
        }
        if (text.all { it.code < 128 }) {
            return "en"
        }
        return "ur"
    }

    private fun getToolsDeclaration(): JSONArray {
        val declarations = JSONArray()

        declarations.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Open an installed Android app such as YouTube, WhatsApp, Instagram, Facebook, Chrome, Calculator, Maps, Settings, Camera, Gallery, etc.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the app to launch")
                    })
                })
                put("required", JSONArray().apply { put("appName") })
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "searchAndCallContact")
            put("description", "Search phonebook contact and initiate a phone call.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The contact's name to call")
                    })
                })
                put("required", JSONArray().apply { put("contactName") })
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "sendWhatsAppMessage")
            put("description", "Open WhatsApp to draft and send a message to a contact.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the contact or phone number")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The text message content")
                    })
                })
                put("required", JSONArray().apply {
                    put("contactName")
                    put("message")
                })
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "getBatteryStatus")
            put("description", "Get the device battery percentage and charging state.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "getStorageInfo")
            put("description", "Get device internal storage free/total statistics.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "getNetworkStatus")
            put("description", "Get network connectivity status (Wi-Fi, Mobile Data, or Offline).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "getDeviceInfo")
            put("description", "Get device hardware brand, model, and Android OS version.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "openWifiSettings")
            put("description", "Open Wi-Fi Settings screen on Android.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "openBluetoothSettings")
            put("description", "Open Bluetooth Settings screen on Android.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "openSoundSettings")
            put("description", "Open Sound and Volume Settings screen on Android.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "increaseVolume")
            put("description", "Increase the device audio/music volume.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("steps", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Number of volume steps to raise")
                    })
                })
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "decreaseVolume")
            put("description", "Decrease the device audio/music volume.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("steps", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Number of volume steps to lower")
                    })
                })
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "setVolumePercent")
            put("description", "Set the device media volume to a specific percentage (0 to 100).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("percent", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Volume percentage from 0 to 100")
                    })
                })
                put("required", JSONArray().apply { put("percent") })
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "maxVolume")
            put("description", "Set device volume to maximum (100%).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "muteVolume")
            put("description", "Mute or silence device volume.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        declarations.put(JSONObject().apply {
            put("name", "toggleFlashlight")
            put("description", "Turn the device torch / flashlight ON or OFF.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("enable", JSONObject().apply {
                        put("type", "BOOLEAN")
                        put("description", "True to turn on flashlight, false to turn off")
                    })
                })
                put("required", JSONArray().apply { put("enable") })
            })
        })

        val toolContainer = JSONObject().apply {
            put("functionDeclarations", declarations)
        }
        return JSONArray().apply { put(toolContainer) }
    }
}

data class ProcessResult(
    val spokenResponse: String,
    val toolExecuted: String? = null,
    val isToolSuccess: Boolean? = null,
    val detectedLanguage: String = "ur"
)
