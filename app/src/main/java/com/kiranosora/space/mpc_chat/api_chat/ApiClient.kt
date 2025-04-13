package com.kiranosora.space.mpc_chat.api_chat
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TIME_OUT = 60L
    // OkHttpClient 实例 (需要先定义)
    val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
            .readTimeout(TIME_OUT, TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
            .build()
    }
    val eventSourceFactory: EventSource.Factory by lazy {
        EventSources.createFactory(okHttpClient) // 使用上面定义的 okHttpClient
    }
}