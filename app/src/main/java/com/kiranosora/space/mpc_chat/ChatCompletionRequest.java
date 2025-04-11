package com.kiranosora.space.mpc_chat;

import java.util.List;

// 简化的请求体示例
public class ChatCompletionRequest {
    String model; // 你想使用的模型名称，可能需要从服务提供商处获取
    List<ChatMessage> messages;
    boolean stream = true;
    // 其他参数如 temperature, max_tokens 等可以按需添加
    public ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }
}
