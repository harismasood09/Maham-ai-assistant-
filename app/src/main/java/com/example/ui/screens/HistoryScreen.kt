package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ConversationEntity
import com.example.data.models.MessageSender
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    conversations: List<ConversationEntity>,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "STARK LOGS // CONVERSATION HISTORY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = JarvisCyanBright
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("history_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = JarvisCyanBright
                        )
                    }
                },
                actions = {
                    if (conversations.isNotEmpty()) {
                        IconButton(onClick = onClearHistory, modifier = Modifier.testTag("history_clear_icon")) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear History",
                                tint = JarvisRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JarvisHudBlack)
            )
        },
        containerColor = JarvisHudBlack
    ) { paddingValues ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(JarvisHudBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "[ NO PROTOCOL LOGS RECORDED ]",
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "کوئی گفتگو ریکارڈ نہیں ہوئی۔",
                        color = JarvisTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
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
                    .testTag("history_list"),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations) { item ->
                    val isUser = item.sender == MessageSender.USER.name
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val timeStr = sdf.format(Date(item.timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.90f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isUser) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF14243B), Color(0xFF1E3554))
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(JarvisHudCard, Color(0xFF091426))
                                        )
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isUser) JarvisCyan.copy(alpha = 0.5f) else JarvisHudCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isUser) JarvisCyan.copy(alpha = 0.2f) else JarvisGold.copy(alpha = 0.2f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                                                contentDescription = null,
                                                tint = if (isUser) JarvisCyanBright else JarvisGold,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isUser) "[ OPERATOR // آپ ]" else "[ J.A.R.V.I.S. // ماہم ]",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) JarvisCyanBright else JarvisGold
                                        )
                                    }

                                    Text(
                                        text = timeStr,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = JarvisTextMuted
                                    )
                                }

                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    color = JarvisTextPrimary,
                                    lineHeight = 19.sp
                                )

                                if (item.toolExecuted != null) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (item.isToolSuccess == true) Color(0x2200FF9D) else Color(0x22FF2A55)
                                            )
                                            .border(
                                                1.dp,
                                                if (item.isToolSuccess == true) JarvisGreen.copy(alpha = 0.5f) else JarvisRed.copy(alpha = 0.5f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (item.isToolSuccess == true) JarvisGreen else JarvisRed,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "ACTION: ${item.toolExecuted}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isToolSuccess == true) JarvisGreen else JarvisRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
