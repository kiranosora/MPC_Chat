package com.kiranosora.space.mpc_chat.api_chat;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

// 示例 Retrofit 接口
public interface ChatApiService {
    @POST("chat/completions") // 假设端点是 chat/completions
    Call<ChatCompletionResponse> createChatCompletion(@Header("Authorization") String apiKey, @Body ChatCompletionRequest request);
}
