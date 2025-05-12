package com.kiranosora.space.mpc_chat.api_chat;

import com.kiranosora.space.mpc_chat.mcp.FunctionTool;

import java.util.List;

// 简化的请求体示例
public class ChatCompletionRequest {
    String model; // 你想使用的模型名称，可能需要从服务提供商处获取
    List<ChatMessage> messages;
    boolean stream = true;
    List<FunctionTool> tools;

    // 其他参数如 temperature, max_tokens 等可以按需添加
    public ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
        this.tools = null;
    }
    public ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream, List<FunctionTool> tools) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
        this.tools = tools;
    }
}
