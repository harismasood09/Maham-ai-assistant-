package com.example.tools

import android.content.Context
import com.example.data.models.ToolExecutionOutcome
import com.example.device.DeviceControlManager
import org.json.JSONObject

class ToolExecutionEngine(private val context: Context) {

    val deviceManager = DeviceControlManager(context)

    enum class RiskLevel {
        LOW,        // Immediate execution (Opening apps, reading battery/info, timers, settings)
        HIGH,       // Requires confirmation or pre-fills draft (Calling, WhatsApp dispatch, SMS, Email)
        RESTRICTED  // Android security boundary or financial safety block
    }

    data class ToolDefinition(
        val name: String,
        val riskLevel: RiskLevel,
        val description: String
    )

    fun getRiskLevel(toolName: String): RiskLevel {
        return when (toolName) {
            "searchAndCallContact", "sendWhatsAppMessage", "prepareSmsMessage", "sendEmail" -> RiskLevel.HIGH
            "refuseFinancialTransaction", "explainSecurityLimitation" -> RiskLevel.RESTRICTED
            else -> RiskLevel.LOW
        }
    }

    /**
     * Execute a tool by name with arguments.
     */
    fun execute(toolName: String, args: JSONObject): ToolExecutionOutcome {
        return when (toolName) {
            // Phone Info Tools
            "getBatteryStatus", "getDeviceBatteryStatus" -> deviceManager.getBatteryStatus()
            "getDeviceInfo" -> deviceManager.getDeviceInfo()
            "getCurrentTime" -> deviceManager.getCurrentTime()
            "getCurrentDate" -> deviceManager.getCurrentDate()
            "getNetworkStatus" -> deviceManager.getNetworkStatus()
            "getStorageInfo" -> deviceManager.getStorageInfo()
            "getVolumeStatus" -> deviceManager.getVolumeStatus()
            "increaseVolume", "volumeUp" -> deviceManager.increaseVolume(args.optInt("steps", 1))
            "decreaseVolume", "volumeDown" -> deviceManager.decreaseVolume(args.optInt("steps", 1))
            "setVolumePercent", "setVolume" -> deviceManager.setVolumePercent(args.optInt("percent", 50))
            "maxVolume", "fullVolume" -> deviceManager.maxVolume()
            "muteVolume", "silenceVolume" -> deviceManager.muteVolume()
            "toggleFlashlight" -> deviceManager.toggleFlashlight(args.optBoolean("enable", true))
            "turnOnFlashlight" -> deviceManager.toggleFlashlight(true)
            "turnOffFlashlight" -> deviceManager.toggleFlashlight(false)
            "getConnectionStatus" -> deviceManager.getConnectionStatus()

            // Settings Tools
            "openSettings" -> {
                val typeStr = args.optString("type", "GENERAL")
                val type = try {
                    DeviceControlManager.SettingsType.valueOf(typeStr.uppercase())
                } catch (e: Exception) {
                    DeviceControlManager.SettingsType.GENERAL
                }
                deviceManager.openSettingsScreen(type)
            }
            "openWifiSettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.WIFI)
            "openBluetoothSettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.BLUETOOTH)
            "openSoundSettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.SOUND)
            "openBatterySettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.BATTERY)
            "openAppSettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.APPLICATIONS)
            "openPermissionSettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.PERMISSIONS)
            "openDisplaySettings" -> deviceManager.openSettingsScreen(DeviceControlManager.SettingsType.DISPLAY)

            // App Launching & Web
            "openApp" -> {
                val appName = args.optString("appName", "")
                deviceManager.openApp(appName)
            }
            "searchYouTube" -> {
                val query = args.optString("query", "")
                deviceManager.searchYouTube(query)
            }
            "searchWeb" -> {
                val query = args.optString("query", "")
                deviceManager.searchWeb(query)
            }
            "openMaps" -> {
                val destination = args.optString("destination", "")
                deviceManager.openMaps(destination)
            }
            "openPlayStore" -> {
                val query = args.optString("query", "")
                deviceManager.openPlayStore(query.ifBlank { null })
            }
            "setAlarm" -> {
                val hour = args.optInt("hour", 7)
                val minutes = args.optInt("minutes", 0)
                val title = args.optString("title", "Maham Alarm")
                deviceManager.setAlarm(hour, minutes, title)
            }
            "setTimer" -> {
                val seconds = args.optInt("seconds", 60)
                val label = args.optString("label", "Maham Timer")
                deviceManager.setTimer(seconds, label)
            }

            // Camera & Gallery
            "openCamera" -> deviceManager.openCamera()
            "openGallery" -> deviceManager.openGallery()
            "shareContent" -> {
                val text = args.optString("text", "")
                val title = args.optString("title", "Share")
                deviceManager.shareContent(text, title)
            }

            // High-Risk Communication (Calls, WhatsApp, SMS, Email)
            "searchAndCallContact" -> {
                val contactName = args.optString("contactName", "")
                val direct = args.optBoolean("directCall", true)
                val callResult = deviceManager.prepareOrPlaceCall(contactName, direct)
                callResult.outcome
            }
            "sendWhatsAppMessage" -> {
                val contactName = args.optString("contactName", "")
                val message = args.optString("message", "")
                deviceManager.prepareWhatsAppMessage(contactName, message)
            }
            "prepareSmsMessage", "sendSms" -> {
                val contactOrPhone = args.optString("contactOrPhone", "")
                val message = args.optString("message", "")
                deviceManager.prepareSmsMessage(contactOrPhone, message)
            }
            "sendEmail", "sendGmail" -> {
                val email = args.optString("recipientEmail", "")
                val subject = args.optString("subject", "Message")
                val body = args.optString("body", "")
                deviceManager.sendEmail(email, subject, body)
            }

            // Security Explanations & Financial Protection
            "explainSecurityLimitation" -> {
                val actionName = args.optString("actionName", "Action")
                deviceManager.explainSecurityLimitation(actionName)
            }
            "refuseFinancialTransaction" -> {
                val recipient = args.optString("recipient", "Contact")
                val amount = args.optString("amount", "رقم")
                deviceManager.refuseFinancialTransaction(recipient, amount)
            }

            else -> {
                ToolExecutionOutcome(
                    toolName = toolName,
                    success = false,
                    userFeedback = "نامعلوم حکم۔",
                    details = "Tool not found: $toolName"
                )
            }
        }
    }
}
