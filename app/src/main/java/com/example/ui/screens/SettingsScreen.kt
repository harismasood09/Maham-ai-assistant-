package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.LanguageMode
import com.example.data.models.UserPreferences
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisHudBlack
import com.example.ui.theme.JarvisHudCard
import com.example.ui.theme.JarvisHudCardBorder
import com.example.ui.theme.JarvisHudDark
import com.example.ui.theme.JarvisHudSurface
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentPreferences: UserPreferences,
    onSavePreferences: (UserPreferences) -> Unit,
    onTestVoice: (pitch: Float, rate: Float, lang: String) -> Unit,
    onIncreaseVolume: () -> Unit = {},
    onDecreaseVolume: () -> Unit = {},
    onMaxVolume: () -> Unit = {},
    onMuteVolume: () -> Unit = {},
    onOpenSoundSettings: () -> Unit = {},
    onToggleFlashlight: (Boolean) -> Unit = {},
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    var selectedLanguage by remember(currentPreferences) { mutableStateOf(currentPreferences.languageMode) }
    var wakeWordEnabled by remember(currentPreferences) { mutableStateOf(currentPreferences.wakeWordEnabled) }
    var bgServiceEnabled by remember(currentPreferences) { mutableStateOf(currentPreferences.backgroundServiceEnabled) }
    var voicePitch by remember(currentPreferences) { mutableFloatStateOf(currentPreferences.voicePitch) }
    var speechRate by remember(currentPreferences) { mutableFloatStateOf(currentPreferences.speechRate) }
    var customApiKey by remember(currentPreferences) { mutableStateOf(currentPreferences.customApiKey) }
    var isFlashlightOn by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "STARK PROTOCOL // SETTINGS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = JarvisCyanBright
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisCyanBright
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisHudBlack)
            )
        },
        containerColor = JarvisHudBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            JarvisHudBlack,
                            JarvisHudDark,
                            JarvisHudBlack
                        )
                    )
                )
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_scroll_container"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Language Preference
            JarvisSettingsCard(title = "LANGUAGE PROTOCOL // زبان", icon = Icons.Default.Language, tint = JarvisCyanBright) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LanguageMode.entries.forEach { mode ->
                        val isSelected = selectedLanguage == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) JarvisCyan.copy(alpha = 0.15f) else JarvisHudCard)
                                .border(
                                    1.dp,
                                    if (isSelected) JarvisCyanBright else JarvisHudCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedLanguage = mode
                                    onSavePreferences(
                                        currentPreferences.copy(
                                            languageMode = mode,
                                            wakeWordEnabled = wakeWordEnabled,
                                            backgroundServiceEnabled = bgServiceEnabled,
                                            voicePitch = voicePitch,
                                            speechRate = speechRate,
                                            customApiKey = customApiKey
                                        )
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.displayName,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) JarvisCyanBright else JarvisTextPrimary
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = JarvisCyanBright,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Voice Tuning
            JarvisSettingsCard(title = "AUDIO MODULATION // آواز", icon = Icons.Default.RecordVoiceOver, tint = JarvisGold) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "FEMALE PITCH // آواز کا سُر", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = JarvisTextSecondary)
                        Text(text = String.format("%.2f", voicePitch), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = JarvisGold, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = voicePitch,
                        onValueChange = {
                            voicePitch = it
                            onSavePreferences(currentPreferences.copy(voicePitch = it))
                        },
                        valueRange = 0.8f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisGold,
                            activeTrackColor = JarvisGold
                        ),
                        modifier = Modifier.testTag("pitch_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "SPEECH RATE // بولنے کی رفتار", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = JarvisTextSecondary)
                        Text(text = String.format("%.2f", speechRate), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = JarvisGold, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            onSavePreferences(currentPreferences.copy(speechRate = it))
                        },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisGold,
                            activeTrackColor = JarvisGold
                        ),
                        modifier = Modifier.testTag("rate_slider")
                    )

                    Button(
                        onClick = { onTestVoice(voicePitch, speechRate, selectedLanguage.code) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisGold.copy(alpha = 0.2f))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = JarvisGold)
                        Text(
                            text = "TEST J.A.R.V.I.S. VOICE (آواز سنیں)",
                            fontFamily = FontFamily.Monospace,
                            color = JarvisGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            // 3. Device Audio & Volume Control // والیم کنٹرول
            JarvisSettingsCard(title = "DEVICE AUDIO & HARDWARE // والیم اور ہارڈویئر", icon = Icons.Default.VolumeUp, tint = JarvisCyanBright) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quickly adjust hardware speaker volume and system sound settings:",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onIncreaseVolume,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = JarvisCyanBright, modifier = Modifier.size(16.dp))
                            Text("VOL +", fontFamily = FontFamily.Monospace, color = JarvisCyanBright, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                        }

                        Button(
                            onClick = onDecreaseVolume,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.15f))
                        ) {
                            Icon(imageVector = Icons.Default.VolumeDown, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                            Text("VOL -", fontFamily = FontFamily.Monospace, color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                        }

                        Button(
                            onClick = onMaxVolume,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisGold.copy(alpha = 0.15f))
                        ) {
                            Text("MAX 100%", fontFamily = FontFamily.Monospace, color = JarvisGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onMuteVolume,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisRed.copy(alpha = 0.15f))
                        ) {
                            Icon(imageVector = Icons.Default.VolumeOff, contentDescription = null, tint = JarvisRed, modifier = Modifier.size(14.dp))
                            Text("MUTE", fontFamily = FontFamily.Monospace, color = JarvisRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isFlashlightOn = !isFlashlightOn
                                onToggleFlashlight(isFlashlightOn)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFlashlightOn) JarvisGold.copy(alpha = 0.3f) else JarvisHudCard
                            )
                        ) {
                            Icon(imageVector = Icons.Default.FlashlightOn, contentDescription = null, tint = if (isFlashlightOn) JarvisGold else JarvisTextSecondary, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (isFlashlightOn) "TORCH ON" else "TORCH OFF",
                                fontFamily = FontFamily.Monospace,
                                color = if (isFlashlightOn) JarvisGold else JarvisTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Button(
                            onClick = onOpenSoundSettings,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisHudCard)
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(15.dp))
                            Text(
                                text = "SOUND SETTINGS",
                                fontFamily = FontFamily.Monospace,
                                color = JarvisTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // 4. Wake Word & Background Assistant
            JarvisSettingsCard(title = "WAKE WORD & BACKGROUND // پس منظر", icon = Icons.Default.Tune, tint = JarvisGreen) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Wake Word: 'Maham' (ماہم)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary)
                            Text(text = "Activate assistant hands-free with wake word", fontSize = 11.sp, color = JarvisTextSecondary)
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = {
                                wakeWordEnabled = it
                                onSavePreferences(currentPreferences.copy(wakeWordEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisGreen, checkedTrackColor = JarvisGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Background Protocol (سروس)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = JarvisTextPrimary)
                            Text(text = "Keep assistant active in background notification", fontSize = 11.sp, color = JarvisTextSecondary)
                        }
                        Switch(
                            checked = bgServiceEnabled,
                            onCheckedChange = {
                                bgServiceEnabled = it
                                onSavePreferences(currentPreferences.copy(backgroundServiceEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = JarvisGreen, checkedTrackColor = JarvisGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("bg_service_switch")
                        )
                    }
                }
            }

            // 4. Gemini AI Key (Optional Override)
            JarvisSettingsCard(title = "GEMINI AI CORE KEY // اے آئی", icon = Icons.Default.Key, tint = JarvisCyanBright) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Google AI Studio API key can be set in Secrets panel or entered here:",
                        fontSize = 11.sp,
                        color = JarvisTextSecondary
                    )
                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = {
                            customApiKey = it
                            onSavePreferences(currentPreferences.copy(customApiKey = it))
                        },
                        placeholder = { Text("Enter Gemini API Key (optional)", color = JarvisTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyanBright,
                            unfocusedBorderColor = JarvisHudCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        singleLine = true
                    )
                }
            }

            // 5. Clear History
            Button(
                onClick = onClearHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("clear_history_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF2A55))
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = JarvisRed)
                Text(
                    text = "CLEAR LOGS // گفتگو کا ریکارڈ صاف کریں",
                    color = JarvisRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun JarvisSettingsCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisHudSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(JarvisHudCardBorder, JarvisHudCardBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint.copy(alpha = 0.15f))
                        .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = JarvisTextPrimary
                )
            }
            content()
        }
    }
}
