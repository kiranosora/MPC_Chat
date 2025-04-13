package com.kiranosora.space.mpc_chat;

import com.google.gson.annotations.SerializedName;
import com.kiranosora.space.mpc_chat.db.DbChatMessage;

import java.util.List;

import kotlin.jvm.Transient;

public class ChatMessage {
    long id = 0;
    long sessionId = 0;
    String role; // "user", "assistant", "system"
    String content;
    boolean isStreaming;
    long timestamp ;

    public ChatMessage(long sessionId, String role, String content) {
        this.id = 0;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.isStreaming = true;
        this.timestamp = System.currentTimeMillis();
    }
    public ChatMessage(long sessionId, String role, String content, long timestamp) {
        this.id = 0;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.isStreaming = true;
        this.timestamp = timestamp;
    }

    public ChatMessage(long sessionId, String role, String content, boolean isStreaming) {
        this.id = 0;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.isStreaming = isStreaming;
        this.timestamp = System.currentTimeMillis();
    }
    public ChatMessage(long sessionId, String role, String content, boolean isStreaming, long timestamp) {
        this.id = 0;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.isStreaming = isStreaming;
        this.timestamp = timestamp;
    }

    // (可选) 如果你需要一个通用的、完全复制的方法
    public ChatMessage copy(boolean isStreaming) {
        return new ChatMessage(this.sessionId, this.role, this.content, isStreaming, this.timestamp);
    }

    public DbChatMessage toDbChatMessage() {
        return new DbChatMessage(this.id, this.sessionId, this.role, this.content, this.timestamp);
    }
}

class StreamingChatCompletionChunk {
    String id;
    List<StreamChoice> choices;
    long created;
    String model;
    @SerializedName("system_fingerprint")
    String systemFingerprint;
    String object; // e.g., "chat.completion.chunk"

    public StreamingChatCompletionChunk(String id, List<StreamChoice> choices, long created, String model, String systemFingerprint, String object) {
        this.id = id;
        this.choices = choices;
        this.created = created;
        this.model = model;
        this.systemFingerprint = systemFingerprint;
    }
}



class StreamChoice{
    Delta delta;
    @SerializedName("finish_reason") String finishReason;
    int index;
    @SerializedName("logprobs") Object logprobs;
    public StreamChoice(Delta delta, int index) {
        this.delta = delta;
        this.index = index;

    }
}

class Delta{
    String role;
    String content;
    public Delta(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
