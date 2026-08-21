package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionManager {

    val REQUIRED_PERMISSIONS: List<String> = buildList {
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun getPermissionExplanation(permission: String): PermissionInfo {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> PermissionInfo(
                title = "MICROPHONE // مائیکروفون",
                explanationUrdu = "ماہم سے براہِ راست بات چیت اور وائس کمانڈز کے لیے درکار ہے۔",
                explanationEnglish = "Required for live voice commands and speech recognition.",
                isCritical = true
            )
            Manifest.permission.READ_CONTACTS -> PermissionInfo(
                title = "CONTACTS // فون کے رابطے",
                explanationUrdu = "رابطوں کو تلاش کرنے، کال ملانے یا واٹس ایپ میسج تیار کرنے کے لیے۔",
                explanationEnglish = "Required to search contacts for calling and messaging.",
                isCritical = false
            )
            Manifest.permission.CALL_PHONE -> PermissionInfo(
                title = "PHONE CALLS // براہِ راست کال",
                explanationUrdu = "آپ کے حکم پر منتخب رابطے کو براہِ راست کال ملانے کے لیے۔",
                explanationEnglish = "Required to dial verified contacts hands-free.",
                isCritical = false
            )
            Manifest.permission.POST_NOTIFICATIONS -> PermissionInfo(
                title = "NOTIFICATIONS // نوٹیفیکیشنز",
                explanationUrdu = "پس منظر میں ماہم کے فعال رہنے اور اسٹیٹس الرٹس کے لیے۔",
                explanationEnglish = "Required for background HUD assistant and status alerts.",
                isCritical = false
            )
            else -> PermissionInfo(
                title = "SYSTEM PERMISSION",
                explanationUrdu = "اس فیچر کے لیے سسٹم کی اجازت درکار ہے۔",
                explanationEnglish = "System permission required for this feature.",
                isCritical = false
            )
        }
    }
}

data class PermissionInfo(
    val title: String,
    val explanationUrdu: String,
    val explanationEnglish: String,
    val isCritical: Boolean
)
