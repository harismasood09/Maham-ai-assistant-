package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.BatteryManager
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

class DeviceToolEngine(private val context: Context) {

    /**
     * Search contacts matching the given name query.
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

    /**
     * Open installed app or known package.
     */
    fun openApp(appName: String, specifiedPackage: String? = null): ToolExecutionOutcome {
        val lower = appName.lowercase(Locale.ROOT).trim()

        val targetPackage = specifiedPackage ?: when {
            "youtube" in lower || "یوٹیوب" in lower -> "com.google.android.youtube"
            "whatsapp" in lower || "واٹس ایپ" in lower -> "com.whatsapp"
            "instagram" in lower || "انسٹاگرام" in lower -> "com.instagram.android"
            "facebook" in lower || "فیس بک" in lower -> "com.facebook.katana"
            "chrome" in lower || "کروم" in lower || "browser" in lower -> "com.android.chrome"
            "map" in lower || "نقشہ" in lower || "گوگل میپ" in lower -> "com.google.android.apps.maps"
            "calculator" in lower || "حساب" in lower || "کیلکولیٹر" in lower -> "com.google.android.calculator"
            "gmail" in lower || "ای میل" in lower || "email" in lower -> "com.google.android.gm"
            "clock" in lower || "الارم" in lower || "alarm" in lower -> "com.google.android.deskclock"
            else -> null
        }

        // Special system intents
        if (lower.contains("setting") || lower.contains("سیٹنگ")) {
            return openSettings()
        }
        if (lower.contains("camera") || lower.contains("کیمرہ") || lower.contains("کیمرا") || lower.contains("عکس")) {
            return openCamera()
        }
        if (lower.contains("gallery") || lower.contains("گیلری") || lower.contains("تصویر") || lower.contains("photos")) {
            return openGallery()
        }

        if (targetPackage != null) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return ToolExecutionOutcome(
                    toolName = "openApp",
                    success = true,
                    userFeedback = "Opening $appName...",
                    details = "Launched package $targetPackage"
                )
            }
        }

        // Search in all installed launchable applications
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
                            userFeedback = "Opening ${app.loadLabel(pm)}...",
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
            userFeedback = "App '$appName' could not be found on this device.",
            details = "Package not installed"
        )
    }

    /**
     * Search contact and initiate call.
     */
    fun searchAndCallContact(contactName: String, directCall: Boolean = true): ToolExecutionOutcome {
        val contacts = findContacts(contactName)
        if (contacts.isEmpty()) {
            // Fallback: Open dialer with searched name/number or prompt
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ToolExecutionOutcome(
                toolName = "searchAndCallContact",
                success = false,
                userFeedback = "Could not find contact '$contactName' in phonebook. Opened dialer.",
                details = "No matching contact"
            )
        }

        val targetContact = contacts.first()
        val phoneUri = Uri.parse("tel:${targetContact.phoneNumber.trim()}")

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
            ToolExecutionOutcome(
                toolName = "searchAndCallContact",
                success = true,
                userFeedback = "Calling ${targetContact.name} (${targetContact.phoneNumber})...",
                details = "Contact: ${targetContact.name}"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "searchAndCallContact",
                success = false,
                userFeedback = "Failed to place call: ${e.localizedMessage}",
                details = e.message
            )
        }
    }

    /**
     * Send WhatsApp message with prefilled text to contact or phone.
     */
    fun sendWhatsAppMessage(contactName: String, message: String): ToolExecutionOutcome {
        val contacts = findContacts(contactName)
        val phoneNumber = if (contacts.isNotEmpty()) {
            contacts.first().phoneNumber.replace("[^0-9+]".toRegex(), "")
        } else {
            contactName.replace("[^0-9+]".toRegex(), "")
        }

        val formattedPhone = if (phoneNumber.startsWith("03") && phoneNumber.length == 11) {
            "92" + phoneNumber.substring(1)
        } else {
            phoneNumber.removePrefix("+")
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
                // Try opening in general browser or standard view
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }

            val targetName = if (contacts.isNotEmpty()) contacts.first().name else contactName
            ToolExecutionOutcome(
                toolName = "sendWhatsAppMessage",
                success = true,
                userFeedback = "Opening WhatsApp to send message to $targetName: \"$message\"",
                details = "WhatsApp pre-fill sent"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "sendWhatsAppMessage",
                success = false,
                userFeedback = "Unable to open WhatsApp: ${e.localizedMessage}",
                details = e.message
            )
        }
    }

    /**
     * Send email via Gmail / default email client.
     */
    fun sendGmail(recipientEmail: String, subject: String, body: String): ToolExecutionOutcome {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "sendGmail",
                success = true,
                userFeedback = "Drafted email to $recipientEmail with subject: '$subject'",
                details = "Email client opened"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "sendGmail",
                success = false,
                userFeedback = "Could not open email application.",
                details = e.message
            )
        }
    }

    /**
     * Search & play on YouTube.
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
                userFeedback = "Searching YouTube for '$query'...",
                details = "Query: $query"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "searchYouTube",
                success = false,
                userFeedback = "Failed to launch YouTube.",
                details = e.message
            )
        }
    }

    /**
     * Open Google Maps with destination query.
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
                val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(destination)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            }
            ToolExecutionOutcome(
                toolName = "openMaps",
                success = true,
                userFeedback = "Opening Google Maps for '$destination'...",
                details = "Destination: $destination"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openMaps",
                success = false,
                userFeedback = "Unable to open Google Maps.",
                details = e.message
            )
        }
    }

    /**
     * Get Device Battery Status.
     */
    fun getDeviceBatteryStatus(): ToolExecutionOutcome {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            val isCharging = batteryManager?.isCharging ?: false
            val chargeStatus = if (isCharging) "charging (چارج ہو رہا ہے)" else "on battery (بیٹری پر ہے)"

            ToolExecutionOutcome(
                toolName = "getDeviceBatteryStatus",
                success = true,
                userFeedback = "Your phone battery is at $batteryLevel% and $chargeStatus.",
                details = "Level: $batteryLevel%, Charging: $isCharging"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "getDeviceBatteryStatus",
                success = false,
                userFeedback = "Could not determine battery status.",
                details = e.message
            )
        }
    }

    /**
     * Get current formatted time.
     */
    fun getCurrentTime(): ToolExecutionOutcome {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = sdf.format(Date())
        return ToolExecutionOutcome(
            toolName = "getCurrentTime",
            success = true,
            userFeedback = "The current time is $formattedTime (ابھی کا وقت $formattedTime ہے).",
            details = formattedTime
        )
    }

    /**
     * Get current formatted date.
     */
    fun getCurrentDate(): ToolExecutionOutcome {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val formattedDate = sdf.format(Date())
        return ToolExecutionOutcome(
            toolName = "getCurrentDate",
            success = true,
            userFeedback = "Today is $formattedDate (آج کی تاریخ $formattedDate ہے).",
            details = formattedDate
        )
    }

    /**
     * Set alarm.
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
                userFeedback = "Alarm set for $timeStr with label '$title' (الارم $timeStr کے لیے سیٹ ہو گیا).",
                details = "Alarm set for $timeStr"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "setAlarm",
                success = false,
                userFeedback = "Failed to set alarm: ${e.localizedMessage}",
                details = e.message
            )
        }
    }

    /**
     * Set timer in seconds.
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
            val feedback = if (minutes > 0) "$minutes minute(s)" else "$seconds second(s)"
            ToolExecutionOutcome(
                toolName = "setTimer",
                success = true,
                userFeedback = "Timer started for $feedback ($feedback کا ٹائمر شروع ہو گیا).",
                details = "Timer: $seconds sec"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "setTimer",
                success = false,
                userFeedback = "Failed to start timer.",
                details = e.message
            )
        }
    }

    /**
     * Search the web using Google.
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
                userFeedback = "Searching web for '$query'...",
                details = "Web search launched"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "searchWeb",
                success = false,
                userFeedback = "Could not perform web search.",
                details = e.message
            )
        }
    }

    /**
     * Open Camera.
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
                userFeedback = "Opening Camera (کیمرہ کھول رہی ہوں)...",
                details = "Camera intent launched"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openCamera",
                success = false,
                userFeedback = "Unable to open camera.",
                details = e.message
            )
        }
    }

    /**
     * Open Gallery / Photos.
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
                userFeedback = "Opening Gallery (گیلری کھول رہی ہوں)...",
                details = "Gallery opened"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openGallery",
                success = false,
                userFeedback = "Unable to open gallery.",
                details = e.message
            )
        }
    }

    /**
     * Open Device Settings.
     */
    fun openSettings(): ToolExecutionOutcome {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionOutcome(
                toolName = "openSettings",
                success = true,
                userFeedback = "Opening Device Settings (سیٹنگز کھول رہی ہوں)...",
                details = "Settings opened"
            )
        } catch (e: Exception) {
            ToolExecutionOutcome(
                toolName = "openSettings",
                success = false,
                userFeedback = "Unable to open settings.",
                details = e.message
            )
        }
    }

    /**
     * Safety refuse direct financial transaction.
     */
    fun refuseFinancialTransaction(recipient: String, amount: String): ToolExecutionOutcome {
        return ToolExecutionOutcome(
            toolName = "refuseFinancialTransaction",
            success = true,
            userFeedback = "میں سیکیورٹی وجوہات کی بنا پر براہِ راست رقم ٹرانسفر نہیں کر سکتی، لیکن میں Easypaisa، JazzCash یا متعلقہ بینکنگ ایپ کھولنے میں مدد کر سکتی ہوں۔",
            details = "Financial safety block for $amount to $recipient"
        )
    }
}
