package com.kiranosora.space.mpc_chat.mcp
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Url


public interface MpcCallService {
    @GET("/openapi.json")
    fun getMpcInfo(): Call<MpcInfoResponse>

    @POST
    fun callToolInt(@Url url: String, @Body arguments: Map<String, Int>): Call<String>

    @POST
    fun callToolString(@Url url: String, @Body arguments: Map<String, String>): Call<String>

    @POST
    fun callToolVoid(@Url url: String): Call<TimeResponse>
}

data class TimeResponse(
    @SerializedName("local_time") val localTime: String
)