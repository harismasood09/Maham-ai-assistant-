package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisHudCard
import com.example.ui.theme.JarvisHudCardBorder
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisTextPrimary

data class QuickActionItem(
    val title: String,
    val command: String,
    val icon: ImageVector,
    val tint: Color,
    val tag: String
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        QuickActionItem("والیم بڑھاؤ", "Maham volume barhao", Icons.Default.VolumeUp, JarvisCyanBright, "SYS-VOL-UP"),
        QuickActionItem("والیم کم کرو", "Maham volume kam karo", Icons.Default.VolumeDown, JarvisCyan, "SYS-VOL-DN"),
        QuickActionItem("فلیش لائٹ / ٹارچ", "Maham flashlight on karo", Icons.Default.FlashlightOn, JarvisGold, "SYS-TORCH"),
        QuickActionItem("YouTube کھولو", "Maham YouTube kholo", Icons.Default.PlayArrow, JarvisRed, "SYS-01"),
        QuickActionItem("Ali کو کال کرو", "Maham Ali ko call karo", Icons.Default.Call, JarvisGreen, "SYS-02"),
        QuickActionItem("پښتو خبرې وکړه", "Maham ma sara Pashto ke khabare oka", Icons.Default.Language, JarvisCyanBright, "SYS-03"),
        QuickActionItem("WhatsApp میسج", "Maham WhatsApp par Ali ko message bhejo", Icons.AutoMirrored.Filled.Send, JarvisGreen, "SYS-04"),
        QuickActionItem("بیٹری اسٹیٹس", "Maham battery level batao", Icons.Default.BatteryChargingFull, JarvisGold, "SYS-05"),
        QuickActionItem("اسٹوریج چیک کرو", "Maham storage info batao", Icons.Default.SdStorage, JarvisCyanBright, "SYS-06"),
        QuickActionItem("وائی فائی سیٹنگز", "Maham Wi-Fi settings kholo", Icons.Default.Wifi, JarvisCyan, "SYS-07"),
        QuickActionItem("کیمرہ کھولو", "Maham Camera kholo", Icons.Default.CameraAlt, JarvisGold, "SYS-08"),
        QuickActionItem("گوگل میپس", "Maham Google Maps kholo", Icons.Default.Map, JarvisGold, "SYS-09"),
        QuickActionItem("کیلکولیٹر", "Maham Calculator kholo", Icons.Default.Calculate, JarvisCyanBright, "SYS-10"),
        QuickActionItem("الارم لگاؤ", "Maham subah 7 baje ka alarm lagao", Icons.Default.Alarm, JarvisAmberLocal, "SYS-11"),
        QuickActionItem("سیٹنگز کھولو", "Maham Settings kholo", Icons.Default.Settings, JarvisTextPrimary, "SYS-12"),
        QuickActionItem("کوئی لطیفہ سناؤ", "Maham koi mazaqia joke sunao", Icons.Default.QuestionAnswer, JarvisCyan, "SYS-13")
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_action_chips"),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickActions) { item ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                JarvisHudCard,
                                Color(0xFF071428)
                            )
                        )
                    )
                    .border(1.dp, JarvisHudCardBorder, RoundedCornerShape(8.dp))
                    .clickable { onActionClick(item.command) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.tint,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisTextPrimary
                    )
                }
            }
        }
    }
}

private val JarvisAmberLocal = Color(0xFFFB8500)
