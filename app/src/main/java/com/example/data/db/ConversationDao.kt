package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationEntity): Long

    @Query("DELETE FROM conversation_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM conversation_history ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentMessagesSync(): List<ConversationEntity>
}
