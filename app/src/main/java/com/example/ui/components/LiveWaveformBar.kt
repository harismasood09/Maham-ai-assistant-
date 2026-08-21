package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.AssistantState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisHudCard
import com.example.ui.theme.JarvisHudCardBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextSecondary
import kotlin.random.Random

/**
 * Iron Man / J.A.R.V.I.S. Holographic Audio Spectrum Equalizer.
 */
@Composable
fun LiveWaveformBar(
    assistantState: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    maxHeight: Dp = 36.dp
) {
    val isLive = assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING

    val primaryColor = if (assistantState == AssistantState.SPEAKING) JarvisGold else JarvisCyanBright
    val secondaryColor = if (assistantState == AssistantState.SPEAKING) JarvisAmber else JarvisCyan

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Holographic Spectrum Equalizer Frame
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisHudCard.copy(alpha = 0.6f))
                .border(1.dp, JarvisHudCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(maxHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until barCount) {
                val animHeight = remember { Animatable(3f) }

                LaunchedEffect(amplitude, isLive) {
                    if (isLive) {
                        // Center-weighted equalizer bell curve
                        val centerDist = kotlin.math.abs(i - barCount / 2f) / (barCount / 2f)
                        val weight = (1f - centerDist * 0.45f).coerceIn(0.2f, 1f)
                        val randomNoise = 0.4f + Random.nextFloat() * 0.6f
                        val target = (4f + amplitude * 30f * weight * randomNoise).coerceIn(3f, 32f)
                        animHeight.animateTo(
                            targetValue = target,
                            animationSpec = tween(
                                durationMillis = 60 + (i % 4) * 12,
                                easing = LinearEasing
                            )
                        )
                    } else {
                        animHeight.animateTo(
                            targetValue = 3f,
                            animationSpec = tween(durationMillis = 180)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(animHeight.value.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    primaryColor,
                                    secondaryColor,
                                    Color.Transparent
                                )
                            )
                        )
                )

                if (i < barCount - 1) {
                    Box(modifier = Modifier.width(3.dp))
                }
            }
        }

        // Tech HUD Frequency Telemetry
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AUDIO I/O: 48kHz",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisTextMuted
            )
            Text(
                text = "•",
                fontSize = 9.sp,
                color = JarvisCyan.copy(alpha = 0.5f)
            )
            Text(
                text = if (isLive) "SIGNAL: 100%" else "STANDBY",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLive) JarvisCyanBright else JarvisTextMuted
            )
        }
    }
}
