package com.kiranosora.space.mpc_chat.db

import androidx.lifecycle.LiveData

// Simple repository, can be expanded
class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: LiveData<List<ChatSession>> = chatDao.getAllSessions()

    suspend fun getMessagesForSession(sessionId: Long): List<DbChatMessage> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun insertMessage(message: DbChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun startNewSession(): Long {
        val newSession = ChatSession()
        return chatDao.insertSession(newSession)
    }

    suspend fun getLatestSessionId(): Long? {
        return chatDao.getLatestSession()?.id
    }

    suspend fun setSessionTitleIfNeeded(sessionId: Long, firstMessageContent: String) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null && session.title == null) {
            val potentialTitle = firstMessageContent.take(40) // Take first 40 chars as title
            chatDao.updateSessionTitle(sessionId, potentialTitle)
        }
    }
}