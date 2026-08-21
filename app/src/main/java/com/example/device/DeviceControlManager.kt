package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.models.ContactItem
import com.example.data.models.ToolExecutionOutcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceControlManager(private val context: Context) {

    // ==========================================
    // 1. PHONE INFORMATION (Official Android APIs)
    // ==========================================

    /**
     * Get Battery Level and Charging Status
     */
    fun getBatteryStatus(): ToolExecutionOutcome {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 80
            val isCharging = batteryManager?.isCharging ?: false

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSave = powerManager?.isPowerSaveMode ?: false

            val chargingText = if (isCharging) {
                "چارجنگ پر لگا ہوا ہے (Charging)"
            } else {
                "بیٹری موڈ پر ہے (On battery)"
            }

            val powerSaveText = if (isPowerSave) " اور بیٹری سیور آن ہے" else ""

            val feedback = "آپ کے فون کی بیٹری $batteryLevel فیصد ہے، $chargingText$powerSaveText۔"

            ToolExecutionOutcome(
                toolName = "getBatteryStatus",
                success = true,
                userFeedback = feedback,
                details = "Level: $batteryLevel%, Charging: $isCharging, PowerSave: $isPowerSave"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "getBatteryStatus",
                success = false,
                userFeedback = "بیٹری کی معلومات حاصل کرنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Get Device Hardware and System Info
     */
    fun getDeviceInfo(): ToolExecutionOutcome {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = Build.MODEL
        val androidVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val brand = Build.BRAND.replaceFirstChar { it.uppercase() }

        val feedback = "یہ $brand $model ڈیوائس ہے، جس میں Android $androidVersion (API $sdkInt) چل رہا ہے۔"

        return ToolExecutionOutcome(
            toolName = "getDeviceInfo",
            success = true,
            userFeedback = feedback,
            details = "Manufacturer: $manufacturer, Brand: $brand, Model: $model, Android: $androidVersion"
        )
    }

    /**
     * Get Current Time
     */
    fun getCurrentTime(): ToolExecutionOutcome {
        val sdf12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = sdf12.format(Date())
        return ToolExecutionOutcome(
            toolName = "getCurrentTime",
            success = true,
            userFeedback = "ابھی کا وقت $formattedTime ہے۔",
            details = formattedTime
        )
    }

    /**
     * Get Current Date
     */
    fun getCurrentDate(): ToolExecutionOutcome {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        return ToolExecutionOutcome(
            toolName = "getCurrentDate",
            success = true,
            userFeedback = "آج کی تاریخ $formattedDate ہے۔",
            details = formattedDate
        )
    }

    /**
     * Get Network & Internet Connection Status
     */
    fun getNetworkStatus(): ToolExecutionOutcome {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)

            val isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isMetered = cm?.isActiveNetworkMetered == true

            val typeStr = when {
                isWifi -> "Wi-Fi نیٹ ورک سے منسلک (Connected to Wi-Fi)"
                isCellular -> "موبائل ڈیٹا سے منسلک (Connected to Mobile Data)"
                isConnected -> "انٹرنیٹ سے منسلک"
                else -> "انٹرنیٹ سے منقطع (Offline / Disconnected)"
            }

            val meteredStr = if (isMetered) " (Metered Connection)" else ""
            val feedback = "نیٹ ورک کا اسٹیٹس: $typeStr$meteredStr۔"

            ToolExecutionOutcome(
                toolName = "getNetworkStatus",
                success = true,
                userFeedback = feedback,
                details = "Connected: $isConnected, Wi-Fi: $isWifi, Cellular: $isCellular, Metered: $isMetered"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "getNetworkStatus",
                success = false,
                userFeedback = "نیٹ ورک کی معلومات دستیاب نہیں۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Get Device Internal Storage Info
     */
    fun getStorageInfo(): ToolExecutionOutcome {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - availableBytes

            val totalGB = totalBytes.toDouble() / (1024 * 1024 * 1024)
            val availableGB = availableBytes.toDouble() / (1024 * 1024 * 1024)
            val usedGB = usedBytes.toDouble() / (1024 * 1024 * 1024)
            val usedPercent = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            val feedback = String.format(
                Locale.getDefault(),
                "آپ کے فون میں %.1f GB اسٹوریج خالی ہے (کل %.1f GB میں سے، %d%% استعمال شدہ)۔",
                availableGB,
                totalGB,
                usedPercent
            )

            ToolExecutionOutcome(
                toolName = "getStorageInfo",
                success = true,
                userFeedback = feedback,
                details = String.format(Locale.ROOT, "Free: %.2f GB, Total: %.2f GB, Used: %.2f GB (%d%%)", availableGB, totalGB, usedGB, usedPercent)
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "getStorageInfo",
                success = false,
                userFeedback = "اسٹوریج کی معلومات معلوم نہیں ہو سکیں۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Get Volume and Ringer Status
     */
    fun getVolumeStatus(): ToolExecutionOutcome {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                return ToolExecutionOutcome(
                    toolName = "getVolumeStatus",
                    success = false,
                    userFeedback = "آڈیو مینیجر دستیاب نہیں ہے۔",
                    details = "AudioManager null"
                )
            }

            val mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val mediaPercent = if (maxMedia > 0) ((mediaVolume.toFloat() / maxMedia) * 100).toInt() else 0

            val ringVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

            val ringerMode = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "خاموش (Silent)"
                AudioManager.RINGER_MODE_VIBRATE -> "وائبریشن (Vibrate)"
                else -> "نارمل رنگ (Normal Ring)"
            }

            val feedback = "میڈیا والیم $mediaPercent فیصد ہے اور فون $ringerMode موڈ پر ہے۔"

            ToolExecutionOutcome(
                toolName = "getVolumeStatus",
                success = true,
                userFeedback = feedback,
                details = "Media: $mediaVolume/$maxMedia ($mediaPercent%), Ring: $ringVolume/$maxRing, Mode: $ringerMode"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "getVolumeStatus",
                success = false,
                userFeedback = "والیم کا اسٹیٹس معلوم نہیں ہو سکا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Increase Media / Voice Volume
     */
    fun increaseVolume(steps: Int = 1): ToolExecutionOutcome {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                return ToolExecutionOutcome("increaseVolume", false, "آڈیو مینیجر دستیاب نہیں ہے۔")
            }
            repeat(steps.coerceIn(1, 5)) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = if (max > 0) ((current.toFloat() / max) * 100).toInt() else 0
            ToolExecutionOutcome(
                toolName = "increaseVolume",
                success = true,
                userFeedback = "والیم بڑھا دیا گیا ہے ($percent فیصد)۔",
                details = "Volume increased to $current/$max ($percent%)"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome("increaseVolume", false, "والیم تبدیل کرنے میں مسئلہ پیش آیا۔", e.localizedMessage)
        }
    }

    /**
     * Decrease Media / Voice Volume
     */
    fun decreaseVolume(steps: Int = 1): ToolExecutionOutcome {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                return ToolExecutionOutcome("decreaseVolume", false, "آڈیو مینیجر دستیاب نہیں ہے۔")
            }
            repeat(steps.coerceIn(1, 5)) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = if (max > 0) ((current.toFloat() / max) * 100).toInt() else 0
            ToolExecutionOutcome(
                toolName = "decreaseVolume",
                success = true,
                userFeedback = "والیم کم کر دیا گیا ہے ($percent فیصد)۔",
                details = "Volume decreased to $current/$max ($percent%)"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome("decreaseVolume", false, "والیم کم کرنے میں مسئلہ پیش آیا۔", e.localizedMessage)
        }
    }

    /**
     * Set specific volume percentage (0..100)
     */
    fun setVolumePercent(percent: Int): ToolExecutionOutcome {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                return ToolExecutionOutcome("setVolumePercent", false, "آڈیو مینیجر دستیاب نہیں ہے۔")
            }
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val clamped = percent.coerceIn(0, 100)
            val target = ((clamped.toFloat() / 100f) * max).toInt().coerceIn(0, max)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            ToolExecutionOutcome(
                toolName = "setVolumePercent",
                success = true,
                userFeedback = "والیم $clamped فیصد پر سیٹ کر دیا گیا ہے۔",
                details = "Volume set to $target/$max ($clamped%)"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome("setVolumePercent", false, "والیم سیٹ کرنے میں ناکامی ہوئی۔", e.localizedMessage)
        }
    }

    /**
     * Max Volume (100%)
     */
    fun maxVolume(): ToolExecutionOutcome {
        return setVolumePercent(100).copy(
            toolName = "maxVolume",
            userFeedback = "والیم فل (100%) کر دیا گیا ہے۔"
        )
    }

    /**
     * Mute Volume (0%)
     */
    fun muteVolume(): ToolExecutionOutcome {
        return setVolumePercent(0).copy(
            toolName = "muteVolume",
            userFeedback = "والیم میوٹ (خاموش) کر دیا گیا ہے۔"
        )
    }

    /**
     * Toggle Flashlight / Torch
     */
    fun toggleFlashlight(enable: Boolean): ToolExecutionOutcome {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                if (cameraManager != null && cameraId != null) {
                    cameraManager.setTorchMode(cameraId, enable)
                    val statusText = if (enable) "آن (روشن) کر دی گئی ہے" else "آف (بند) کر دی گئی ہے"
                    ToolExecutionOutcome(
                        toolName = "toggleFlashlight",
                        success = true,
                        userFeedback = "ٹارچ / فلیش لائٹ $statusText۔",
                        details = "Torch state: $enable"
                    )
                } else {
                    ToolExecutionOutcome("toggleFlashlight", false, "اس ڈیوائس میں فلیش لائٹ دستیاب نہیں ہے۔")
                }
            } else {
                ToolExecutionOutcome("toggleFlashlight", false, "اینڈرائیڈ ورژن پر فلیش لائٹ سپورٹڈ نہیں ہے۔")
            }
        } catch (e: Exception) {
            ToolExecutionOutcome("toggleFlashlight", false, "فلیش لائٹ کو کنٹرول کرنے میں مسئلہ پیش آیا۔", e.localizedMessage)
        }
    }

    /**
     * Get Overall Connection Status
     */
    fun getConnectionStatus(): ToolExecutionOutcome {
        val netOutcome = getNetworkStatus()
        val batteryOutcome = getBatteryStatus()
        val volumeOutcome = getVolumeStatus()

        val summary = "${netOutcome.userFeedback} ${batteryOutcome.userFeedback} ${volumeOutcome.userFeedback}"
        return ToolExecutionOutcome(
            toolName = "getConnectionStatus",
            success = true,
            userFeedback = summary,
            details = "Network: ${netOutcome.details} | Battery: ${batteryOutcome.details} | Volume: ${volumeOutcome.details}"
        )
    }

    // ==========================================
    // 2. DEVICE SETTINGS (Official Android APIs)
    // ==========================================

    enum class SettingsType {
        GENERAL,
        WIFI,
        BLUETOOTH,
        APPLICATIONS,
        SOUND,
        BATTERY,
        PERMISSIONS,
        DISPLAY,
        DATE_TIME,
        LOCATION,
        ACCESSIBILITY
    }

    fun openSettingsScreen(type: SettingsType): ToolExecutionOutcome {
        val (action, nameUrdu, nameEn) = when (type) {
            SettingsType.GENERAL -> Triple(Settings.ACTION_SETTINGS, "سیٹنگز", "General Settings")
            SettingsType.WIFI -> Triple(Settings.ACTION_WIFI_SETTINGS, "وائی فائی سیٹنگز", "Wi-Fi Settings")
            SettingsType.BLUETOOTH -> Triple(Settings.ACTION_BLUETOOTH_SETTINGS, "بلوٹوتھ سیٹنگز", "Bluetooth Settings")
            SettingsType.APPLICATIONS -> Triple(Settings.ACTION_APPLICATION_SETTINGS, "ایپس سیٹنگز", "Apps Settings")
            SettingsType.SOUND -> Triple(Settings.ACTION_SOUND_SETTINGS, "ساؤنڈ اور والیم سیٹنگز", "Sound Settings")
            SettingsType.BATTERY -> Triple(Settings.ACTION_BATTERY_SAVER_SETTINGS, "بیٹری سیٹنگز", "Battery Settings")
            SettingsType.PERMISSIONS -> Triple(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, "ایپ پرمیشن سیٹنگز", "Permission Settings")
            SettingsType.DISPLAY -> Triple(Settings.ACTION_DISPLAY_SETTINGS, "ڈسپلے سیٹنگز", "Display Settings")
            SettingsType.DATE_TIME -> Triple(Settings.ACTION_DATE_SETTINGS, "تاریخ اور وقت سیٹنگز", "Date & Time Settings")
            SettingsType.LOCATION -> Triple(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "لوکیشن سیٹنگز", "Location Settings")
            SettingsType.ACCESSIBILITY -> Triple(Settings.ACTION_ACCESSIBILITY_SETTINGS, "ایکسیسیبلٹی سیٹنگز", "Accessibility Settings")
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "openSettingsScreen",
                success = true,
                userFeedback = "$nameUrdu کھول رہی ہوں ($nameEn)...",
                details = "Opened $action"
            )
        } catch (e: Exception) {
            // Fallback to general settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ToolExecutionOutcome(
                    toolName = "openSettingsScreen",
                    success = true,
                    userFeedback = "مین سیٹنگز کھول رہی ہوں...",
                    details = "Fallback to general settings"
                )
            } catch (ex: Exception) {
                ToolExecutionOutcome(
                    toolName = "openSettingsScreen",
                    success = false,
                    userFeedback = "سیٹنگز کھولنے میں ناکامی ہوئی۔",
                    details = ex.localizedMessage
                )
            }
        }
    }

    // ==========================================
    // 3. APPS & WEB LAUNCHING
    // ==========================================

    /**
     * Open installed application by name or package
     */
    fun openApp(appName: String): ToolExecutionOutcome {
        val lower = appName.lowercase(Locale.ROOT).trim()

        // Match common settings intents
        when {
            "wifi" in lower || "wi-fi" in lower || "وائی فائی" in lower -> return openSettingsScreen(SettingsType.WIFI)
            "bluetooth" in lower || "بلوٹوتھ" in lower -> return openSettingsScreen(SettingsType.BLUETOOTH)
            "sound" in lower || "volume" in lower || "آواز" in lower -> return openSettingsScreen(SettingsType.SOUND)
            "battery" in lower || "بیٹری" in lower -> return openSettingsScreen(SettingsType.BATTERY)
            "setting" in lower || "سیٹنگ" in lower -> return openSettingsScreen(SettingsType.GENERAL)
            "camera" in lower || "کیمرہ" in lower || "کیمرا" in lower -> return openCamera()
            "gallery" in lower || "گیلری" in lower || "photos" in lower || "تصاویر" in lower -> return openGallery()
            "play store" in lower || "playstore" in lower || "پلے اسٹور" in lower -> return openPlayStore()
        }

        // Known top packages
        val knownPackage = when {
            "youtube" in lower || "یوٹیوب" in lower || "یوټیوب" in lower -> "com.google.android.youtube"
            "whatsapp" in lower || "واٹس ایپ" in lower || "واټساپ" in lower -> "com.whatsapp"
            "instagram" in lower || "انسٹاگرام" in lower || "انسٹا" in lower -> "com.instagram.android"
            "facebook" in lower || "فیس بک" in lower -> "com.facebook.katana"
            "chrome" in lower || "کروم" in lower || "browser" in lower -> "com.android.chrome"
            "map" in lower || "maps" in lower || "نقشہ" in lower || "گوگل میپ" in lower -> "com.google.android.apps.maps"
            "calculator" in lower || "کیلکولیٹر" in lower || "حساب" in lower -> "com.google.android.calculator"
            "gmail" in lower || "ای میل" in lower || "email" in lower -> "com.google.android.gm"
            "clock" in lower || "الارم" in lower || "alarm" in lower -> "com.google.android.deskclock"
            else -> null
        }

        if (knownPackage != null) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(knownPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return ToolExecutionOutcome(
                    toolName = "openApp",
                    success = true,
                    userFeedback = "جی، $appName کھول رہی ہوں۔",
                    details = "Launched $knownPackage"
                )
            }
        }

        // Dynamic query: Search all installed launcher applications
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            for (app in apps) {
                val label = app.loadLabel(pm).toString().lowercase(Locale.ROOT)
                if (label.contains(lower) || lower.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ToolExecutionOutcome(
                            toolName = "openApp",
                            success = true,
                            userFeedback = "جی، ${app.loadLabel(pm)} کھول رہی ہوں۔",
                            details = "Launched ${app.activityInfo.packageName}"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ToolExecutionOutcome(
            toolName = "openApp",
            success = false,
            userFeedback = "معذرت، ایپ '$appName' اس فون میں نہیں ملی۔",
            details = "Package not installed"
        )
    }

    /**
     * Search & Open YouTube
     */
    fun searchYouTube(query: String): ToolExecutionOutcome {
        return try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
            } else {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ToolExecutionOutcome(
                toolName = "searchYouTube",
                success = true,
                userFeedback = "یوٹیوب پر '$query' تلاش کر رہی ہوں۔",
                details = "Query: $query"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "searchYouTube",
                success = false,
                userFeedback = "یوٹیوب کھولنے میں ناکامی ہوئی۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Search Google Web
     */
    fun searchWeb(query: String): ToolExecutionOutcome {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "searchWeb",
                success = true,
                userFeedback = "گوگل پر سرچ کر رہی ہوں: $query",
                details = "Web search query: $query"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "searchWeb",
                success = false,
                userFeedback = "ویب سرچ شروع کرنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Open Google Maps
     */
    fun openMaps(destination: String): ToolExecutionOutcome {
        return try {
            val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val genericIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://maps.google.com/?q=${Uri.encode(destination)}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            }
            ToolExecutionOutcome(
                toolName = "openMaps",
                success = true,
                userFeedback = "گوگل میپس پر $destination کا راستہ دکھا رہی ہوں۔",
                details = "Destination: $destination"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openMaps",
                success = false,
                userFeedback = "گوگل میپس کھولنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Open Google Play Store
     */
    fun openPlayStore(query: String? = null): ToolExecutionOutcome {
        return try {
            val uri = if (!query.isNullOrBlank()) {
                Uri.parse("market://search?q=${Uri.encode(query)}")
            } else {
                Uri.parse("market://details?id=com.google.android.youtube")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = if (!query.isNullOrBlank()) {
                    Uri.parse("https://play.google.com/store/search?q=${Uri.encode(query)}")
                } else {
                    Uri.parse("https://play.google.com/store")
                }
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ToolExecutionOutcome(
                toolName = "openPlayStore",
                success = true,
                userFeedback = if (query != null) "پلے اسٹور پر $query تلاش کر رہی ہوں۔" else "گوگل پلے اسٹور کھول رہی ہوں۔",
                details = "Play Store opened"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openPlayStore",
                success = false,
                userFeedback = "پلے اسٹور کھولنے میں ناکامی ہوئی۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Set Alarm
     */
    fun setAlarm(hour: Int, minutes: Int, title: String = "Maham Reminder"): ToolExecutionOutcome {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_MESSAGE, title)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minutes)
            ToolExecutionOutcome(
                toolName = "setAlarm",
                success = true,
                userFeedback = "الارم $timeStr بجے کے لیے سیٹ ہو گیا ہے ('$title')۔",
                details = "Alarm: $timeStr, Title: $title"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "setAlarm",
                success = false,
                userFeedback = "الارم سیٹ کرنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Set Timer in seconds
     */
    fun setTimer(seconds: Int, label: String = "Maham Timer"): ToolExecutionOutcome {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val minutes = seconds / 60
            val timeDesc = if (minutes > 0) "$minutes منٹ" else "$seconds سیکنڈ"
            ToolExecutionOutcome(
                toolName = "setTimer",
                success = true,
                userFeedback = "$timeDesc کا ٹائمر شروع کر دیا گیا ہے۔",
                details = "Timer: $seconds sec"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "setTimer",
                success = false,
                userFeedback = "ٹائمر شروع کرنے میں ناکامی ہوئی۔",
                details = e.localizedMessage
            )
        }
    }

    // ==========================================
    // 4. CONTACTS & PHONE CALLS
    // ==========================================

    /**
     * Search contacts in Android phonebook
     */
    fun findContacts(query: String): List<ContactItem> {
        val contactsList = mutableListOf<ContactItem>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            return contactsList
        }

        try {
            val contentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        contactsList.add(ContactItem(id, name, number))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactsList
    }

    data class CallPreparationResult(
        val outcome: ToolExecutionOutcome,
        val matchedContacts: List<ContactItem>,
        val requiresClarification: Boolean
    )

    /**
     * Prepare or initiate a phone call respecting contacts and ambiguous matches
     */
    fun prepareOrPlaceCall(contactQuery: String, directCall: Boolean = true): CallPreparationResult {
        val contacts = findContacts(contactQuery)

        // Case 1: No match found in phonebook
        if (contacts.isEmpty()) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return CallPreparationResult(
                outcome = ToolExecutionOutcome(
                    toolName = "searchAndCallContact",
                    success = false,
                    userFeedback = "فون بک میں '$contactQuery' کا کوئی نمبر نہیں ملا۔ میں نے ڈائلر کھول دیا ہے۔",
                    details = "No contact match found"
                ),
                matchedContacts = emptyList(),
                requiresClarification = false
            )
        }

        // Case 2: Multiple distinct contacts found ("Which Ali?")
        if (contacts.size > 1) {
            val namesSummary = contacts.take(3).joinToString("، ") { "${it.name} (${it.phoneNumber})" }
            val feedback = "مجھے '$contactQuery' نام کے ${contacts.size} رابطے ملے ہیں: $namesSummary۔ آپ کس کو کال ملانا چاہتے ہیں؟"
            return CallPreparationResult(
                outcome = ToolExecutionOutcome(
                    toolName = "searchAndCallContact",
                    success = true,
                    userFeedback = feedback,
                    details = "Multiple contacts match (${contacts.size})"
                ),
                matchedContacts = contacts,
                requiresClarification = true
            )
        }

        // Case 3: Exactly 1 clear match found!
        val target = contacts.first()
        val phoneUri = Uri.parse("tel:${target.phoneNumber.trim()}")

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (directCall && hasCallPermission) {
            Intent(Intent.ACTION_CALL, phoneUri)
        } else {
            Intent(Intent.ACTION_DIAL, phoneUri)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            CallPreparationResult(
                outcome = ToolExecutionOutcome(
                    toolName = "searchAndCallContact",
                    success = true,
                    userFeedback = "${target.name} کو کال ملا رہی ہوں (${target.phoneNumber})...",
                    details = "Call initiated for ${target.name}"
                ),
                matchedContacts = contacts,
                requiresClarification = false
            )
        } catch (e: Exception) {
            CallPreparationResult(
                outcome = ToolExecutionOutcome(
                    toolName = "searchAndCallContact",
                    success = false,
                    userFeedback = "کال ملانے میں مسئلہ پیش آیا: ${e.localizedMessage}",
                    details = e.message
                ),
                matchedContacts = contacts,
                requiresClarification = false
            )
        }
    }

    // ==========================================
    // 5. MESSAGES & WHATSAPP
    // ==========================================

    /**
     * Prepare SMS / Message via official Android intent
     */
    fun prepareSmsMessage(contactOrPhone: String, message: String): ToolExecutionOutcome {
        val contacts = findContacts(contactOrPhone)
        val targetNumber = if (contacts.isNotEmpty()) {
            contacts.first().phoneNumber
        } else {
            contactOrPhone.replace("[^0-9+]".toRegex(), "")
        }

        return try {
            val uri = if (targetNumber.isNotBlank()) Uri.parse("smsto:$targetNumber") else Uri.parse("sms:")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            val name = if (contacts.isNotEmpty()) contacts.first().name else contactOrPhone
            ToolExecutionOutcome(
                toolName = "prepareSmsMessage",
                success = true,
                userFeedback = "$name کے لیے میسج تیار کر دیا ہے: \"$message\"",
                details = "SMS Intent launched"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "prepareSmsMessage",
                success = false,
                userFeedback = "میسجنگ ایپ کھولنے میں ناکامی ہوئی۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Prepare WhatsApp Message via official intent / deep link
     */
    fun prepareWhatsAppMessage(contactOrPhone: String, message: String): ToolExecutionOutcome {
        val contacts = findContacts(contactOrPhone)
        val rawNumber = if (contacts.isNotEmpty()) {
            contacts.first().phoneNumber.replace("[^0-9+]".toRegex(), "")
        } else {
            contactOrPhone.replace("[^0-9+]".toRegex(), "")
        }

        val formattedPhone = if (rawNumber.startsWith("03") && rawNumber.length == 11) {
            "92" + rawNumber.substring(1)
        } else {
            rawNumber.removePrefix("+")
        }

        return try {
            val url = if (formattedPhone.isNotBlank() && formattedPhone.length >= 10) {
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }

            val targetName = if (contacts.isNotEmpty()) contacts.first().name else contactOrPhone
            ToolExecutionOutcome(
                toolName = "sendWhatsAppMessage",
                success = true,
                userFeedback = "واٹس ایپ پر $targetName کے لیے میسج کھول دیا ہے: \"$message\"",
                details = "WhatsApp intent dispatched"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "sendWhatsAppMessage",
                success = false,
                userFeedback = "واٹس ایپ کھولنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Send Email via official mail client
     */
    fun sendEmail(recipientEmail: String, subject: String, body: String): ToolExecutionOutcome {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "sendEmail",
                success = true,
                userFeedback = "$recipientEmail کے لیے ای میل کا ڈرافٹ کھول دیا ہے۔",
                details = "Email client opened"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "sendEmail",
                success = false,
                userFeedback = "ای میل ایپ کھولنے میں ناکامی ہوئی۔",
                details = e.localizedMessage
            )
        }
    }

    // ==========================================
    // 6. CAMERA & GALLERY
    // ==========================================

    /**
     * Open Camera
     */
    fun openCamera(): ToolExecutionOutcome {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "openCamera",
                success = true,
                userFeedback = "کیمرہ کھول رہی ہوں۔ ذرا مسکرا دیجیے!",
                details = "Camera launched"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openCamera",
                success = false,
                userFeedback = "کیمرہ کھولنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Open Photo Gallery
     */
    fun openGallery(): ToolExecutionOutcome {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "openGallery",
                success = true,
                userFeedback = "گیلری کھول رہی ہوں۔",
                details = "Gallery opened"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openGallery",
                success = false,
                userFeedback = "گیلری کھولنے میں مسئلہ پیش آیا۔",
                details = e.localizedMessage
            )
        }
    }

    /**
     * Share Media or Text via official Android Share Sheet
     */
    fun shareContent(text: String, title: String = "Share via Maham"): ToolExecutionOutcome {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
            ToolExecutionOutcome(
                toolName = "shareContent",
                success = true,
                userFeedback = "شیئر شیٹ کھول دی گئی ہے۔",
                details = "Shared content: $text"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "shareContent",
                success = false,
                userFeedback = "شیئر کرنے میں ناکامی ہوئی۔",
                details = e.localizedMessage
            )
        }
    }

    // ==========================================
    // 7. SECURITY EXPLANATION & REFUSAL
    // ==========================================

    /**
     * Explain security restrictions when an action cannot be performed directly
     */
    fun explainSecurityLimitation(actionName: String, relevantSettings: SettingsType? = null): ToolExecutionOutcome {
        if (relevantSettings != null) {
            openSettingsScreen(relevantSettings)
        }
        val feedback = "یہ کام Android کی security کی وجہ سے براہِ راست نہیں ہو سکتا، لیکن میں اس کی settings کھول سکتی ہوں۔"
        return ToolExecutionOutcome(
            toolName = "explainSecurityLimitation",
            success = true,
            userFeedback = feedback,
            details = "Security boundary respected for $actionName"
        )
    }

    /**
     * Safely refuse financial transactions
     */
    fun refuseFinancialTransaction(recipient: String, amount: String): ToolExecutionOutcome {
        val feedback = "میں سیکیورٹی وجوہات کی بنا پر براہِ راست رقم ٹرانسفر نہیں کر سکتی، لیکن میں Easypaisa، JazzCash یا بینکنگ ایپ کھولنے میں مدد کر سکتی ہوں۔"
        return ToolExecutionOutcome(
            toolName = "refuseFinancialTransaction",
            success = true,
            userFeedback = feedback,
            details = "Financial safety block for $amount to $recipient"
        )
    }
}
