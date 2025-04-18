package com.kiranosora.space.mpc_chat.mpc

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kiranosora.space.mpc_chat.MpcConfig
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Objects

class MpcRequestHelper {
    companion object {
        val retorfit: Retrofit.Builder = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
        val gson = Gson()
        var functionTools : List<FunctionTool>? = null
        fun createGetMpcInfoRequest(currentMpcConfig: MpcConfig): List<FunctionTool>? {
            val mpcCallService =retorfit.baseUrl(currentMpcConfig.baseUrl).build().create(MpcCallService::class.java)
            val mpcInfoResponse =  mpcCallService.getMpcInfo().execute().body()
            functionTools = mpcInfoResponse?.toFunctionTool()!!
            val functionToolsJson = gson.toJson(functionTools)
            Log.d("MPC_${currentMpcConfig.name}", "Function Tools: $functionToolsJson")
            return functionTools
        }

        fun createMpcToolCallRequest(currentMpcConfig: MpcConfig, functionCallArguments: FunctionCallArguments): String? {
            val mpcCallService =retorfit.baseUrl(currentMpcConfig.baseUrl).build().create(MpcCallService::class.java)
            Log.d("tool_call", "arguments: ${functionCallArguments.arguments}")
            var type = object : TypeToken<Map<String, Object>>() {}.type
            if(functionTools == null){
                return "null functionTools"
            }
            if(functionTools!!.isEmpty()){
                return "null functionTools"
            }
            var arg_types = mutableListOf<String>()
            for(tool in functionTools){
                Log.d("MpcToolCall", "${tool.functionDetails?.name}  ${functionCallArguments.name}")
                if(tool.functionDetails?.name?.substring(1) == functionCallArguments.name || tool.functionDetails?.name == functionCallArguments.name){
                    val arg_map = tool.functionDetails?.parameters?.properties
                    for((key, value) in arg_map!!){
                        if(value.type != null){
                            arg_types.add(value.type)
                        }
                    }
                }
            }
            if(arg_types.isEmpty()){
                return "empty arg_types for tool ${functionCallArguments.name}"
            }
            val toolCallResponse:String
            if(arg_types[0] == "string"){
                type = object : TypeToken<Map<String, String>>() {}.type
                val arguments: Map<String, String> = gson.fromJson(functionCallArguments.arguments, type)
                Log.d("tool_call", "start to call arguments: $arguments with url = ${currentMpcConfig.baseUrl}/${functionCallArguments.name}")
                toolCallResponse = mpcCallService.callToolString("${currentMpcConfig.baseUrl}/${functionCallArguments.name}",
                    arguments
                ).execute().body().toString()
            }
            else if(arg_types[0] == "integer"){
                type = object : TypeToken<Map<String, Int>>() {}.type
                val arguments: Map<String, Int> = gson.fromJson(functionCallArguments.arguments, type)
                toolCallResponse = mpcCallService.callToolInt("${currentMpcConfig.baseUrl}/${functionCallArguments.name}",
                    arguments
                ).execute().body().toString()
            }
            else{
                return "${arg_types[0]} not implemented yet"
            }

            Log.d("MPC_${currentMpcConfig.name}", "Function Tools: $toolCallResponse")
            return toolCallResponse
        }

        fun createMpcSystemPrompt(currentMpcConfig: MpcConfig) : List<FunctionTool>?{
            val functionTools = createGetMpcInfoRequest(currentMpcConfig)
            return functionTools
        }

        fun getFunctionCall(data: String): ToolChatCompletionChunk? {
            return gson.fromJson(data, ToolChatCompletionChunk::class.java)
        }

        fun addFunctionCall(chunks: List<ToolChatCompletionChunk>, delaChunk: ToolChatCompletionChunk): List<ToolChatCompletionChunk> {
            val function = delaChunk.choices?.get(0)?.delta?.toolCalls?.get(0)?.function
            val index = delaChunk.choices?.get(0)?.index
            var tool_index = delaChunk.choices?.get(0)?.delta?.toolCalls?.get(0)?.index
            if( chunks.isEmpty()) return listOf(delaChunk)
            if(function != null && index != null && tool_index != null){
                var toolcalls = chunks[index].choices?.get(0)?.delta?.toolCalls?.toMutableList()
                if(toolcalls != null){
                    if(tool_index >= toolcalls.size){
                        tool_index = toolcalls.size -1
                        Log.e("function_tool", "tool_index:$tool_index, toolcalls.size: $toolcalls.size")
                    }
                    toolcalls[tool_index].function?.name = toolcalls[tool_index].function?.name + function.name
                    toolcalls[tool_index].function?.arguments = toolcalls[tool_index].function?.arguments + function.arguments
                }
            }
            return chunks
        }
    }
    class BaseGetMpcInfoListener(val currentMpcConfig: MpcConfig): EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            super.onOpen(eventSource, response)
            Log.d("MPC_${currentMpcConfig.name}", "Connection Opened")
        }

        override fun onEvent(
            eventSource: EventSource,
            id: String?,
            type: String?,
            data: String
        ) {
            super.onEvent(eventSource, id, type, data)
            //Log.d("MPC_${currentMpcConfig.name}", "Event Received: $data")
            val mpcInfoResponse = gson.fromJson(data, MpcInfoResponse::class.java)
            val functionTools = mpcInfoResponse.toFunctionTool()
            val functionToolsJson = gson.toJson(functionTools)
            Log.d("MPC_${currentMpcConfig.name}", "Function Tools: $functionToolsJson")
        }

        override fun onFailure(
            eventSource: EventSource,
            t: Throwable?,
            response: Response?
        ) {
            super.onFailure(eventSource, t, response)
            Log.e("MPC_${currentMpcConfig.name}", "Connection Failed with error: ${t?.message} response:${response?.message}")
        }

        override fun onClosed(eventSource: EventSource) {
            super.onClosed(eventSource)
            Log.d("MPC_${currentMpcConfig.name}", "Connection Closed")
        }
    }
}