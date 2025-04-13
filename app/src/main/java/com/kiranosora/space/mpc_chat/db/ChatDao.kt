package com.kiranosora.space.mpc_chat.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatDao {
    // --- Session Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long // Returns the new session ID

    @Query("SELECT * FROM chat_sessions ORDER BY start_timestamp DESC")
    fun getAllSessions(): LiveData<List<ChatSession>> // Observe session list changes

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): ChatSession?

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String)

    @Query("SELECT * FROM chat_sessions ORDER BY start_timestamp DESC LIMIT 1")
    suspend fun getLatestSession(): ChatSession? // Get the most recent session

    // --- Message Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DbChatMessage)

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<DbChatMessage> // Get messages for a specific session
}