package com.kiranosora.space.mpc_chat.api_chat

import com.google.gson.Gson

class ApiChatHelper {
    companion object{
        val gson = Gson()
        fun getContentDelta(data:String) :String?{
            val chunk = gson.fromJson(data, StreamingChatCompletionChunk::class.java)
            val contentDelta = chunk?.choices?.firstOrNull()?.delta?.content
            return contentDelta
        }
        fun getFinishReason(data:String) :String?{
            val chunk = gson.fromJson(data, StreamingChatCompletionChunk::class.java)
            return chunk?.getChoices()?.firstOrNull()?.finishReason
        }
    }
}