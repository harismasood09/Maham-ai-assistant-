package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.models.AssistantState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCoreWhite
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Tech Iron Man / J.A.R.V.I.S. Arc Reactor Holographic Visualizer.
 * Features rotating segmented HUD rings, electromagnetic power coils,
 * live sound wave laser arrays, and central plasma core.
 */
@Composable
fun GlowingOrbVisualizer(
    assistantState: AssistantState,
    audioAmplitude: Float,
    modifier: Modifier = Modifier,
    orbSize: Dp = 230.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JarvisHudTransitions")

    // Primary clockwise rotation for outer telemetry ring
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (assistantState == AssistantState.THINKING) 3000 else 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OuterRotation"
    )

    // Counter-clockwise rotation for inner power coils
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (assistantState == AssistantState.THINKING) 2000 else 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "InnerRotation"
    )

    // Arc reactor breathing pulse
    val reactorPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (assistantState == AssistantState.THINKING) 800 else 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ReactorPulse"
    )

    // Smooth amplitude transitions for real-time speech reactivity
    val smoothedAmplitude = remember { Animatable(0f) }
    LaunchedEffect(audioAmplitude) {
        smoothedAmplitude.animateTo(
            targetValue = audioAmplitude,
            animationSpec = tween(durationMillis = 65, easing = LinearEasing)
        )
    }

    // Dynamic JARVIS color schemes based on system state
    val (primaryGlow, accentGlow, coreGlow) = when (assistantState) {
        AssistantState.LISTENING -> Triple(JarvisCyanBright, JarvisCyan, JarvisCoreWhite)
        AssistantState.THINKING -> Triple(JarvisGold, JarvisAmber, JarvisCyanBright)
        AssistantState.SPEAKING -> Triple(JarvisGold, JarvisAmber, JarvisCoreWhite)
        AssistantState.IDLE -> Triple(JarvisCyan, JarvisCyanBright, JarvisCoreWhite)
        AssistantState.ERROR -> Triple(JarvisRed, JarvisAmber, JarvisRed)
        AssistantState.VOICE_OFF -> Triple(Color(0xFF38BDF8).copy(alpha = 0.4f), Color(0xFF0F172A), Color(0xFF1E293B))
    }

    Box(
        modifier = modifier
            .size(orbSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(orbSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.width * 0.28f

            val dynamicScale = when (assistantState) {
                AssistantState.IDLE -> reactorPulse
                AssistantState.LISTENING -> 1f + smoothedAmplitude.value * 0.48f
                AssistantState.SPEAKING -> 1f + smoothedAmplitude.value * 0.52f
                AssistantState.THINKING -> reactorPulse * 1.08f
                AssistantState.ERROR, AssistantState.VOICE_OFF -> 0.92f
            }

            val currentRadius = baseRadius * dynamicScale

            // 1. Holographic Arc Reactor Diffuse Background Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.40f),
                        accentGlow.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 2.1f
                ),
                radius = currentRadius * 2.1f,
                center = center
            )

            // 2. Outer HUD Crosshair Reticles & Corner Marks
            drawHudReticles(center, currentRadius * 1.85f, primaryGlow.copy(alpha = 0.5f))

            // 3. Segmented Arc Reactor Outer Ring (Clockwise)
            drawSegmentedArcRing(
                center = center,
                radius = currentRadius * 1.65f,
                angle = outerRotation,
                color = primaryGlow
            )

            // 4. Calibrated Degree & Telemetry Ticks
            drawDegreeTicks(
                center = center,
                radius = currentRadius * 1.48f,
                color = primaryGlow.copy(alpha = 0.45f)
            )

            // 5. Electromagnetic Power Coils Array (Counter-Clockwise)
            drawPowerCoilArray(
                center = center,
                radius = currentRadius * 1.28f,
                angle = innerRotation,
                color = accentGlow,
                coilCount = 10
            )

            // 6. Real-time Audio Laser Spikes (Listening & Speaking)
            if (assistantState == AssistantState.LISTENING || assistantState == AssistantState.SPEAKING) {
                drawAudioLaserSpikes(
                    center = center,
                    radius = currentRadius * 1.05f,
                    amplitude = smoothedAmplitude.value,
                    color = if (assistantState == AssistantState.SPEAKING) JarvisGold else JarvisCyanBright
                )
            }

            // 7. Central Arc Reactor Core Ring & Housing
            drawArcReactorCore(
                center = center,
                radius = currentRadius,
                primaryColor = primaryGlow,
                accentColor = accentGlow,
                coreColor = coreGlow,
                state = assistantState
            )
        }
    }
}

/**
 * Draws HUD reticles, crosshairs, and corner brackets.
 */
private fun DrawScope.drawHudReticles(center: Offset, radius: Float, color: Color) {
    val crosshairLen = 12.dp.toPx()
    val stroke = 1.5.dp.toPx()

    // 4 cardinal HUD tick marks
    val cardinals = listOf(0.0, PI / 2.0, PI, 3.0 * PI / 2.0)
    for (ang in cardinals) {
        val startX = center.x + (radius - crosshairLen) * cos(ang).toFloat()
        val startY = center.y + (radius - crosshairLen) * sin(ang).toFloat()
        val endX = center.x + (radius + crosshairLen) * cos(ang).toFloat()
        val endY = center.y + (radius + crosshairLen) * sin(ang).toFloat()

        drawLine(
            color = color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = stroke
        )
    }

    // Outer thin HUD guideline circle
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 16f), 0f))
    )
}

/**
 * Draws the heavy segmented arc reactor outer ring.
 */
private fun DrawScope.drawSegmentedArcRing(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color
) {
    val strokeWidth = 3.dp.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(45f, 20f, 15f, 20f), angle)

    drawCircle(
        color = color.copy(alpha = 0.85f),
        radius = radius,
        center = center,
        style = Stroke(width = strokeWidth, pathEffect = pathEffect, cap = StrokeCap.Round)
    )

    // Orbital Energy Particles
    for (i in 0 until 3) {
        val pAngle = Math.toRadians((angle + i * 120.0))
        val dotX = center.x + (radius * cos(pAngle)).toFloat()
        val dotY = center.y + (radius * sin(pAngle)).toFloat()
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(dotX, dotY)
        )
    }
}

/**
 * Draws tech degree calibration tick marks around the reactor.
 */
private fun DrawScope.drawDegreeTicks(
    center: Offset,
    radius: Float,
    color: Color
) {
    val count = 36
    val tickLength = 5.dp.toPx()
    val majorTickLength = 8.dp.toPx()

    for (i in 0 until count) {
        val angleRad = (i.toDouble() / count) * 2 * PI
        val isMajor = i % 9 == 0
        val length = if (isMajor) majorTickLength else tickLength

        val startX = center.x + (radius - length) * cos(angleRad).toFloat()
        val startY = center.y + (radius - length) * sin(angleRad).toFloat()
        val endX = center.x + radius * cos(angleRad).toFloat()
        val endY = center.y + radius * sin(angleRad).toFloat()

        drawLine(
            color = if (isMajor) color.copy(alpha = 0.9f) else color,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
        )
    }
}

/**
 * Draws the 10 electromagnetic power coil blocks of the Arc Reactor.
 */
private fun DrawScope.drawPowerCoilArray(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
    coilCount: Int
) {
    val coilWidth = 14.dp.toPx()
    val coilLength = 8.dp.toPx()

    for (i in 0 until coilCount) {
        val coilAngle = Math.toRadians((angle + (i * 360.0 / coilCount)))
        val cX = center.x + (radius * cos(coilAngle)).toFloat()
        val cY = center.y + (radius * sin(coilAngle)).toFloat()

        drawCircle(
            color = color.copy(alpha = 0.8f),
            radius = 4.dp.toPx(),
            center = Offset(cX, cY)
        )

        // Radial connector line to inner core
        val innerX = center.x + ((radius - coilLength) * cos(coilAngle)).toFloat()
        val innerY = center.y + ((radius - coilLength) * sin(coilAngle)).toFloat()

        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(cX, cY),
            end = Offset(innerX, innerY),
            strokeWidth = 2.dp.toPx()
        )
    }

    // Inner bounding coil ring
    drawCircle(
        color = color.copy(alpha = 0.4f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
}

/**
 * Draws real-time audio laser rays during active conversation.
 */
private fun DrawScope.drawAudioLaserSpikes(
    center: Offset,
    radius: Float,
    amplitude: Float,
    color: Color
) {
    val spikeCount = 32
    for (i in 0 until spikeCount) {
        val angleRad = (i.toDouble() / spikeCount) * 2 * PI
        val ampFactor = (0.5f + ((i * 7) % 11) / 11f)
        val rayLength = (12.dp.toPx() + amplitude * 50.dp.toPx()) * ampFactor

        val startX = center.x + (radius * cos(angleRad)).toFloat()
        val startY = center.y + (radius * sin(angleRad)).toFloat()
        val endX = center.x + ((radius + rayLength) * cos(angleRad)).toFloat()
        val endY = center.y + ((radius + rayLength) * sin(angleRad)).toFloat()

        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Draws the glowing multi-layer Arc Reactor Core & Stark Triangle frame.
 */
private fun DrawScope.drawArcReactorCore(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    accentColor: Color,
    coreColor: Color,
    state: AssistantState
) {
    // Outer Arc Ring Wall
    drawCircle(
        color = Color(0xFF070E1E),
        radius = radius,
        center = center
    )

    drawCircle(
        color = primaryColor,
        radius = radius,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )

    // Glowing Core Plasma
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor,
                primaryColor,
                accentColor,
                Color(0xFF030712)
            ),
            center = center,
            radius = radius * 0.95f
        ),
        radius = radius * 0.95f,
        center = center
    )

    // Inner Stark Triangular Power Housing
    val triangleRadius = radius * 0.58f
    val path = Path().apply {
        val p1 = Offset(center.x, center.y - triangleRadius)
        val p2 = Offset(center.x + triangleRadius * cos(PI / 6.0).toFloat(), center.y + triangleRadius * sin(PI / 6.0).toFloat())
        val p3 = Offset(center.x - triangleRadius * cos(PI / 6.0).toFloat(), center.y + triangleRadius * sin(PI / 6.0).toFloat())

        moveTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
    }

    drawPath(
        path = path,
        color = primaryColor.copy(alpha = 0.5f),
        style = Stroke(width = 2.dp.toPx())
    )

    // Central Stark Core Node
    drawCircle(
        color = coreColor,
        radius = radius * 0.22f,
        center = center
    )

    drawCircle(
        color = primaryColor,
        radius = radius * 0.22f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}
