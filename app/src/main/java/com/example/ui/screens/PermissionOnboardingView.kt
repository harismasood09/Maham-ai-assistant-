package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisHudBlack
import com.example.ui.theme.JarvisHudCard
import com.example.ui.theme.JarvisHudCardBorder
import com.example.ui.theme.JarvisHudSurface
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun PermissionOnboardingView(
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("permission_onboarding_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisHudSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(JarvisCyanBright, JarvisHudCardBorder)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Shield Icon
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(JarvisCyan.copy(alpha = 0.25f), Color.Transparent)))
                    .border(1.5.dp, JarvisCyanBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = JarvisCyanBright,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "STARK PROTOCOL // SYSTEM ACCESS",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = JarvisCyanBright,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "ماہم (J.A.R.V.I.S.) کو مکمل طور پر فعال کرنے کے لیے درج ذیل اجازتیں درکار ہیں:",
                fontSize = 12.sp,
                color = JarvisTextSecondary,
                textAlign = TextAlign.Center
            )

            // Permissions list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = "MICROPHONE // مائیکروفون",
                    description = "Live audio input & 'Maham' wake word detection",
                    tint = JarvisCyanBright
                )
                PermissionItem(
                    icon = Icons.Default.Contacts,
                    title = "CONTACTS // رابطے",
                    description = "Voice search for calling and WhatsApp contacts",
                    tint = JarvisGold
                )
                PermissionItem(
                    icon = Icons.Default.Call,
                    title = "PHONE CALLS // فون کالز",
                    description = "Direct hands-free voice dialing",
                    tint = JarvisGreen
                )
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "NOTIFICATIONS // اطلاعات",
                    description = "Background HUD telemetry and wake state",
                    tint = JarvisAmber
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("grant_permissions_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright)
            ) {
                Text(
                    text = "GRANT PERMISSIONS // اختیارات دیں",
                    fontFamily = FontFamily.Monospace,
                    color = JarvisHudBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("skip_permissions_button"),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(JarvisHudCardBorder, JarvisHudCardBorder)))
            ) {
                Text(
                    text = "CONTINUE IN BASIC MODE // جاری رکھیں",
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisHudCard)
            .border(1.dp, JarvisHudCardBorder, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = JarvisTextPrimary
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = JarvisTextSecondary
            )
        }
    }
}
