package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AssistantState
import com.example.data.models.ToolExecutionOutcome
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisHudCard
import com.example.ui.theme.JarvisHudCardBorder
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

/**
 * Iron Man / J.A.R.V.I.S. Holographic HUD Transcription & Telemetry Card.
 */
@Composable
fun TranscriptionCard(
    assistantState: AssistantState,
    transcription: String,
    lastToolOutcome: ToolExecutionOutcome?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = transcription.isNotBlank() || assistantState != AssistantState.IDLE || lastToolOutcome != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 15 }),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            JarvisHudCard.copy(alpha = 0.95f),
                            Color(0xFF060D1E).copy(alpha = 0.98f)
                        )
                    )
                )
                .border(1.dp, JarvisHudCardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("transcription_card")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // HUD Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val stateColor = when (assistantState) {
                            AssistantState.LISTENING -> JarvisCyanBright
                            AssistantState.THINKING -> JarvisGold
                            AssistantState.SPEAKING -> JarvisAmber
                            AssistantState.IDLE -> JarvisGreen
                            AssistantState.ERROR -> JarvisRed
                            AssistantState.VOICE_OFF -> Color(0xFF64748B)
                        }

                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Text(
                            text = "[ ${assistantState.name} // ${assistantState.urduLabel} ]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = stateColor
                        )
                    }

                    // Multilingual JARVIS HUD tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0C182E))
                            .border(1.dp, JarvisHudCardBorder.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "URDU • PASHTO • EN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = JarvisTextSecondary
                        )
                    }
                }

                // Transcription or Status message
                if (transcription.isNotBlank()) {
                    Text(
                        text = "\"$transcription\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = JarvisTextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else if (assistantState == AssistantState.LISTENING) {
                    Text(
                        text = "J.A.R.V.I.S. listening... بولیں، ماہم آپ کی آواز سن رہی ہے",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = JarvisCyanBright,
                        textAlign = TextAlign.Center
                    )
                } else if (assistantState == AssistantState.THINKING) {
                    Text(
                        text = "Processing query... ماہم جواب تیار کر رہی ہے",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = JarvisGold,
                        textAlign = TextAlign.Center
                    )
                }

                // Tool execution outcome chip
                if (lastToolOutcome != null && assistantState != AssistantState.LISTENING) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (lastToolOutcome.success) Color(0x2200FF9D) else Color(0x22FF2A55)
                            )
                            .border(
                                1.dp,
                                if (lastToolOutcome.success) JarvisGreen.copy(alpha = 0.5f) else JarvisRed.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lastToolOutcome.success) Icons.Default.CheckCircle else Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = if (lastToolOutcome.success) JarvisGreen else JarvisRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "[ EXEC: ${lastToolOutcome.toolName} ]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lastToolOutcome.success) JarvisGreen else JarvisRed
                        )
                    }
                }
            }
        }
    }
}
