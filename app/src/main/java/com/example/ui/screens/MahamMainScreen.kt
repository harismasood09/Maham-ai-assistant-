package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AssistantState
import com.example.ui.components.GlowingOrbVisualizer
import com.example.ui.components.LiveWaveformBar
import com.example.ui.components.QuickActionChips
import com.example.ui.components.TranscriptionCard
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisHudBlack
import com.example.ui.theme.JarvisHudCard
import com.example.ui.theme.JarvisHudCardBorder
import com.example.ui.theme.JarvisHudDark
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun MahamMainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onOpenPermissionSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val assistantState by viewModel.assistantState.collectAsState()
    val transcription by viewModel.currentTranscription.collectAsState()
    val liveAmplitude by viewModel.liveAudioAmplitude.collectAsState()
    val lastToolOutcome by viewModel.lastToolOutcome.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = JarvisHudBlack,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF091730),
                            JarvisHudDark,
                            JarvisHudBlack
                        ),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top HUD Header with Stark J.A.R.V.I.S. Branding & Actions
                JarvisTopHeader(
                    isVoiceActive = userPrefs.voiceModeActive,
                    onToggleVoice = { viewModel.toggleVoiceMode() },
                    onOpenHistory = onNavigateToHistory,
                    onOpenSettings = onNavigateToSettings,
                    onOpenPermissions = onOpenPermissionSheet
                )

                // 2. Central Arc Reactor Stage
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // HUD Status Telemetry Pill
                    JarvisVoiceStatusBanner(
                        isVoiceActive = userPrefs.voiceModeActive,
                        onActivateVoice = { viewModel.toggleVoiceMode(true) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Center Arc Reactor Visualizer
                    GlowingOrbVisualizer(
                        assistantState = assistantState,
                        audioAmplitude = liveAmplitude,
                        orbSize = 215.dp,
                        onClick = {
                            when (assistantState) {
                                AssistantState.LISTENING -> viewModel.stopListening()
                                AssistantState.SPEAKING -> viewModel.interruptSpeech()
                                AssistantState.VOICE_OFF -> viewModel.toggleVoiceMode(true)
                                else -> viewModel.startListening()
                            }
                        },
                        modifier = Modifier.testTag("central_glowing_orb")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // HUD Assistant State Badge
                    JarvisStateBadge(assistantState = assistantState)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live Audio Equalizer Waveform
                    LiveWaveformBar(
                        assistantState = assistantState,
                        amplitude = liveAmplitude,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Holographic Transcription Card
                    TranscriptionCard(
                        assistantState = assistantState,
                        transcription = transcription,
                        lastToolOutcome = lastToolOutcome,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // 3. Bottom HUD Controls Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Action HUD Prompts
                    QuickActionChips(
                        onActionClick = { command ->
                            viewModel.processUserInput(command)
                        }
                    )

                    // Arc Reactor Microphone Controller Button
                    JarvisMicrophoneController(
                        assistantState = assistantState,
                        isVoiceActive = userPrefs.voiceModeActive,
                        onMicClick = {
                            when (assistantState) {
                                AssistantState.LISTENING -> viewModel.stopListening()
                                AssistantState.SPEAKING -> viewModel.interruptSpeech()
                                AssistantState.VOICE_OFF -> viewModel.toggleVoiceMode(true)
                                else -> viewModel.startListening()
                            }
                        }
                    )

                    Text(
                        text = "SAY \"MAHAM\" • STARK HUD // بولیں 'ماہم' یا بٹن دبائیں",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun JarvisTopHeader(
    isVoiceActive: Boolean,
    onToggleVoice: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Branding / Stark HUD
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(JarvisCyan.copy(alpha = 0.2f), Color(0xFF0369A1).copy(alpha = 0.3f))
                        )
                    )
                    .border(1.dp, JarvisCyanBright.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ماہم",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisCyanBright
                )
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "J.A.R.V.I.S.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisCyanBright
                    )
                    Text(
                        text = "// MAHAM",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = JarvisGold
                    )
                }
                Text(
                    text = "STARK AI PROTOCOL • اسسٹنٹ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = JarvisTextSecondary
                )
            }
        }

        // Top HUD Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isVoiceActive) JarvisCyan.copy(alpha = 0.15f) else JarvisHudCard)
                    .border(1.dp, if (isVoiceActive) JarvisCyanBright else JarvisHudCardBorder, RoundedCornerShape(6.dp))
                    .testTag("top_voice_mode_toggle")
            ) {
                Icon(
                    imageVector = if (isVoiceActive) Icons.Default.VolumeUp else Icons.Default.MicOff,
                    contentDescription = "Voice Mode",
                    tint = if (isVoiceActive) JarvisCyanBright else JarvisTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onOpenPermissions,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JarvisHudCard)
                    .border(1.dp, JarvisHudCardBorder, RoundedCornerShape(6.dp))
                    .testTag("top_permissions_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions",
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JarvisHudCard)
                    .border(1.dp, JarvisHudCardBorder, RoundedCornerShape(6.dp))
                    .testTag("top_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = JarvisTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JarvisHudCard)
                    .border(1.dp, JarvisHudCardBorder, RoundedCornerShape(6.dp))
                    .testTag("top_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = JarvisTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun JarvisVoiceStatusBanner(
    isVoiceActive: Boolean,
    onActivateVoice: () -> Unit
) {
    if (!isVoiceActive) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF131D33))
                .border(1.dp, Color(0xFF334E68), RoundedCornerShape(8.dp))
                .clickable { onActivateVoice() }
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = JarvisCyanBright,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "[ SYSTEM STANDBY // TAP TO ACTIVATE VOICE ]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisTextSecondary
            )
        }
    }
}

@Composable
private fun JarvisStateBadge(assistantState: AssistantState) {
    val (stateBg, stateColor) = when (assistantState) {
        AssistantState.LISTENING -> Pair(JarvisCyan.copy(alpha = 0.15f), JarvisCyanBright)
        AssistantState.THINKING -> Pair(JarvisGold.copy(alpha = 0.15f), JarvisGold)
        AssistantState.SPEAKING -> Pair(JarvisAmber.copy(alpha = 0.15f), JarvisAmber)
        AssistantState.IDLE -> Pair(JarvisGreen.copy(alpha = 0.15f), JarvisGreen)
        AssistantState.ERROR -> Pair(Color(0x22FF2A55), JarvisRed)
        AssistantState.VOICE_OFF -> Pair(Color(0x2264748B), Color(0xFF94A3B8))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(stateBg)
            .border(1.dp, stateColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
            Text(
                text = "STATUS: ${assistantState.name} // ${assistantState.urduLabel}",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = stateColor
            )
        }
    }
}

@Composable
private fun JarvisMicrophoneController(
    assistantState: AssistantState,
    isVoiceActive: Boolean,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JarvisMicRipple")

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )

    val isListening = assistantState == AssistantState.LISTENING
    val isSpeaking = assistantState == AssistantState.SPEAKING
    val isVoiceOff = assistantState == AssistantState.VOICE_OFF

    val buttonBrush = when {
        isListening -> Brush.radialGradient(listOf(JarvisCyanBright, Color(0xFF0284C7)))
        isSpeaking -> Brush.radialGradient(listOf(JarvisGold, JarvisAmber))
        isVoiceOff -> Brush.radialGradient(listOf(Color(0xFF334E68), Color(0xFF0F1B2E)))
        else -> Brush.radialGradient(listOf(JarvisCyanBright, Color(0xFF0369A1)))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(86.dp)
    ) {
        // Holographic pulsing HUD ring when listening/speaking
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(rippleScale)
                    .clip(CircleShape)
                    .background((if (isListening) JarvisCyan else JarvisGold).copy(alpha = 0.25f))
            )
        }

        // Main Arc Reactor Controller Button
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(buttonBrush)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .clickable { onMicClick() }
                .testTag("microphone_action_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isSpeaking -> Icons.Default.Stop
                    isListening -> Icons.Default.GraphicEq
                    isVoiceOff -> Icons.Default.MicOff
                    else -> Icons.Default.Mic
                },
                contentDescription = if (isSpeaking) "Stop Speaking" else "Microphone",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
