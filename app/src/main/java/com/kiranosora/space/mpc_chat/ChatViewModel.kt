package com.kiranosora.space.mpc_chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.kiranosora.space.mpc_chat.api_chat.ChatMessage
import com.kiranosora.space.mpc_chat.db.AppDatabase
import com.kiranosora.space.mpc_chat.db.ChatRepository
import com.kiranosora.space.mpc_chat.db.toUiChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    val allSessions: LiveData<List<com.kiranosora.space.mpc_chat.db.ChatSession>> // LiveData for history

    // StateFlow to hold the current session's messages
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages

    // StateFlow to hold the ID of the currently active session
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId

    init {
        val chatDao = AppDatabase.getDatabase(application).chatDao()
        repository = ChatRepository(chatDao)
        allSessions = repository.allSessions

        // Load the last session or start a new one on init
        viewModelScope.launch {
            val latestSessionId = repository.getLatestSessionId()
            if (latestSessionId != null) {
                loadSession(latestSessionId)
            } else {
                startNewSession()
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
            val messages = repository.getMessagesForSession(sessionId)
            _currentMessages.value = messages.map { it.toUiChatMessage() } // Map to UI model
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val newSessionId = repository.startNewSession()
            _currentSessionId.value = newSessionId
            _currentMessages.value = emptyList() // Clear messages for the new session
        }
    }

    // Add a message (user or assistant) to the current session and save it
    fun addMessage(role: String, content: String, sessionId: Long) {
        viewModelScope.launch {
            val isLikelyFirstUserMsgInCurrentState = (role == "user" && _currentMessages.value.none { it.role == "user" })
            val uiMessage = ChatMessage(sessionId,role, content)
            // 2. 立即更新 StateFlow (乐观更新 UI 状态)
            //    确保这是在 viewModelScope.launch 外部，以便立即生效
            Log.d("addMessage", "cuurentMessage before: ${_currentMessages.value}")
            _currentMessages.value = _currentMessages.value + uiMessage
            Log.d("addMessage", "cuurentMessage after: ${_currentMessages.value}")
            val dbMessage = uiMessage.toDbChatMessage()

            // Save to DB
            withContext(Dispatchers.IO) { // Ensure DB operations are off main thread
                if(role == "user" || role == "system" || role == "tool"){
                    repository.insertMessage(dbMessage)
                }
                // Try setting session title based on the first user message
                if(role == "user" && isLikelyFirstUserMsgInCurrentState) {
                    repository.setSessionTitleIfNeeded(sessionId, content)
                }
            }

            Log.d("addMessage", "currentMessages: ${_currentMessages.value.size}, ${_currentMessages.value.lastOrNull()?.content}")
            // Or reload from DB for consistency: loadSession(sessionId)
        }
    }

    // Append streaming content (doesn't save intermediate chunks to DB)
    fun appendStreamingContent(contentChunk: String) {
        //Log.d("appendStreamingContent", "before append messages.size() ${_currentMessages.value.size}")
        val lastMessage = _currentMessages.value.lastOrNull()
//        if (lastMessage != null) {
//            Log.d("appendStreamingContent", "${lastMessage.content}, ${lastMessage.role}, ${lastMessage.isStreaming}")
//        }else{
//            Log.e("appendStreamingContent", "lastMessage is null")
//        }
        if (lastMessage != null && lastMessage.role == "assistant" && lastMessage.isStreaming) {
            lastMessage.content += contentChunk
            val listWithoutLast = _currentMessages.value.dropLast(1)
            val updatedLastMessage = lastMessage.copy(true) // Use copy()
            val newList = listWithoutLast + updatedLastMessage
            _currentMessages.value = newList // Trigger StateFlow update
            //Log.d("appendStreamingContent", "after append messages.size() ${_currentMessages.value.size}")

        }
    }


    // Mark streaming complete and save the final message
    fun markStreamingComplete() {
        viewModelScope.launch {
            val currentList = _currentMessages.value
            val lastMessage = currentList.lastOrNull()
            if (lastMessage != null && lastMessage.role == "assistant" && lastMessage.isStreaming) {
                lastMessage.isStreaming = false // Mark complete
                val dbMessage = lastMessage.toDbChatMessage()
                // Save the final assistant message to DB
                withContext(Dispatchers.IO) {
                    repository.insertMessage(dbMessage)
                }
                // Update the StateFlow reference to ensure recomposition/update
                _currentMessages.value = currentList.toList() // Create a new list instance
            }
        }
    }


    // Build message history for API call from the current StateFlow
    fun buildApiHistory(): List<ChatMessage> {

        return _currentMessages.value
            .filter { it.role == "user" || it.role == "assistant" }
            .map { it.copy(false) } // Use copy if ChatMessage is a data class
            .toList()
        // .takeLast(10) // Limit history if needed
    }
}