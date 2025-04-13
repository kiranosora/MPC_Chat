package com.kiranosora.space.mpc_chat.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.kiranosora.space.mpc_chat.api_chat.ChatMessage

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_timestamp") val startTimestamp: Long = System.currentTimeMillis(),
    // You could add a title later, maybe generated from the first message
    @ColumnInfo(name = "title") var title: String? = null
)

@Entity(
    tableName = "chat_messages",
    // Foreign key to link messages to sessions
    foreignKeys = [ForeignKey(
        entity = ChatSession::class,
        parentColumns = ["id"],
        childColumns = ["session_id"],
        onDelete = ForeignKey.CASCADE // If a session is deleted, delete its messages
    )],
    // Index for faster querying by session_id
    indices = [Index("session_id")]
)
data class DbChatMessage( // Rename to avoid conflict with your network/UI model
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long, // Foreign key reference
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
    // Don't store UI state like 'isStreaming' in the database
)
// Function to map DbChatMessage to UI ChatMessage
fun DbChatMessage.toUiChatMessage(): ChatMessage {
    return ChatMessage(
        this.sessionId,
        this.role,
        this.content,
        false,
        this.timestamp
    )
}


// Helper data class (optional but useful) to load session with messages
 data class SessionWithMessages(
    @Embedded val session: ChatSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val messages: List<DbChatMessage>
 )