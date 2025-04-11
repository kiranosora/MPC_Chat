package com.kiranosora.space.mpc_chat
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.sse.EventSource
import okhttp3.sse.EventSources
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // 使用你提供的 Base URL
    //public const val BASE_URL = "https://kiranosora.space:12345/v1/"
    public const val BASE_URL = "http://192.168.0.110:1234/v1/"
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
    val instance: ChatApiService by lazy {
        // 配置日志拦截器 (可选, 但调试时非常有用)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 可以看到请求体和响应体
        }


        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 使用配置好的 OkHttpClient
            .addConverterFactory(GsonConverterFactory.create()) // 使用 Gson 解析 JSON
            .build()

        retrofit.create(ChatApiService::class.java)
    }
}