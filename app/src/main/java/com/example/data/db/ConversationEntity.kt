package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_history")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "ur",
    val toolExecuted: String? = null,
    val isToolSuccess: Boolean? = null
)
