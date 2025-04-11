package com.kiranosora.space.mpc_chat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import kotlin.jvm.Transient;

public class ChatMessage {
    String role; // "user", "assistant", "system"
    String content;
    boolean isStreaming = false;
    public ChatMessage(String role, String content, boolean isStreaming) {
        this.role = role;
        this.content = content;
        this.isStreaming = isStreaming;
    }
    // (可选) 如果你需要一个通用的、完全复制的方法
    public ChatMessage copy(boolean isStreaming) {
        return new ChatMessage(this.role, this.content, isStreaming);
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
