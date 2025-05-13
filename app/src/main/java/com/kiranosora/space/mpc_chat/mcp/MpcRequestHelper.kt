package com.kiranosora.space.mpc_chat.mcp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kiranosora.space.mpc_chat.McpConfig
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import androidx.core.net.toUri

class MpcRequestHelper {
    companion object {
        private var localFunctions = mutableListOf<FunctionTool>()
        val localFunctionMap = mutableMapOf<String, (Map<String, String>, Context) -> String?>()
        val gson = Gson()
        val mapType = object:TypeToken<Map<String, String>>(){}.type;

        fun openAndroidPermission(arguments:Map<String, String>, context: Context):String?{
            val permission = arguments.get("permission")
            val packageName = arguments.get("packageName")
            val uri = "package:${packageName}".toUri()
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    uri
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
            return "$permission permission for $packageName opened"
        }
        fun getLocalFunctions():List<FunctionTool> {
            if(localFunctions.isNotEmpty()) return localFunctions
            var properties = mutableMapOf<String, ParameterProperty>()
            properties.put("permission", ParameterProperty("string", "permission to open"))
            properties.put("packageName", ParameterProperty("string", "permission of which app package"))

            var required = mutableListOf<String>()
            localFunctions.add(FunctionTool("function", FunctionDetails("openAndroidPermission", "tool to open android permission setting",
                FunctionParameters("object", properties, listOf<String>()))))
            localFunctionMap.put("openAndroidPermission", ::openAndroidPermission)
            return localFunctions
        }


        val retorfit: Retrofit.Builder = Retrofit.Builder().addConverterFactory(GsonConverterFactory.create())
        var functionTools : List<FunctionTool>? = null
        fun createGetMpcInfoRequest(currentMcpConfig: McpConfig): List<FunctionTool>? {
            val mpcCallService =retorfit.baseUrl(currentMcpConfig.baseUrl).build().create(MpcCallService::class.java)
            val mpcInfoResponse =  mpcCallService.getMpcInfo().execute().body()
            functionTools = mpcInfoResponse?.toFunctionTool()!!
            val functionToolsJson = gson.toJson(functionTools)
            Log.d("MPC_${currentMcpConfig.name}", "Function Tools: $functionToolsJson")
            return functionTools
        }


        fun callLocalFunction(functionCallArguments: FunctionCallArguments, context: Context):String?{
            gson.fromJson(functionCallArguments.arguments, Map::class.java)
            return localFunctionMap[functionCallArguments.name]?.invoke(gson.fromJson(functionCallArguments.arguments, mapType), context)
        }

        fun createMpcToolCallRequest(currentMcpConfig: McpConfig, functionCallArguments: FunctionCallArguments, context: Context): String? {
            if(currentMcpConfig.baseUrl == McpConfig.DUMMY){
                return callLocalFunction(functionCallArguments, context)
            }
            val mpcCallService =retorfit.baseUrl(currentMcpConfig.baseUrl).build().create(MpcCallService::class.java)
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

            val toolCallResponse:String
            if(arg_types.isEmpty()){
                Log.d("tool_call", "start to call arguments: void with url = ${currentMcpConfig.baseUrl}/${functionCallArguments.name}")
                toolCallResponse = mpcCallService.callToolVoid(
                    "${currentMcpConfig.baseUrl}${functionCallArguments.name}",
                ).execute().body().toString()            }
            else if(arg_types[0] == "string"){
                type = object : TypeToken<Map<String, String>>() {}.type
                val arguments: Map<String, String> = gson.fromJson(functionCallArguments.arguments, type)
                Log.d("tool_call", "start to call arguments: $arguments with url = ${currentMcpConfig.baseUrl}/${functionCallArguments.name}")
                toolCallResponse = mpcCallService.callToolString("${currentMcpConfig.baseUrl}/${functionCallArguments.name}",
                    arguments
                ).execute().body().toString()
            }
            else if(arg_types[0] == "integer"){
                type = object : TypeToken<Map<String, Int>>() {}.type
                val arguments: Map<String, Int> = gson.fromJson(functionCallArguments.arguments, type)
                toolCallResponse = mpcCallService.callToolInt("${currentMcpConfig.baseUrl}/${functionCallArguments.name}",
                    arguments
                ).execute().body().toString()
            }
            else{
                return "${arg_types[0]} not implemented yet"
            }

            Log.d("MPC_${currentMcpConfig.name}", "Function Tools: $toolCallResponse")
            return toolCallResponse
        }

        fun createMpcSystemPrompt(currentMcpConfig: McpConfig) : List<FunctionTool>?{
            if(currentMcpConfig.baseUrl == McpConfig.DUMMY){
                return getLocalFunctions()
            }
            if(currentMcpConfig.name == McpConfig.DISABLE) return null
            val functionTools = createGetMpcInfoRequest(currentMcpConfig)
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
                    if(toolcalls[tool_index].function?.name?.contains("/") == false) {
                        toolcalls[tool_index].function?.name =
                            toolcalls[tool_index].function?.name + function.name
                        toolcalls[tool_index].function?.arguments =
                            toolcalls[tool_index].function?.arguments + function.arguments
                    }
                }
            }
            return chunks
        }
    }
    class BaseGetMpcInfoListener(val currentMcpConfig: McpConfig): EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            super.onOpen(eventSource, response)
            Log.d("MPC_${currentMcpConfig.name}", "Connection Opened")
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
            Log.d("MPC_${currentMcpConfig.name}", "Function Tools: $functionToolsJson")
        }

        override fun onFailure(
            eventSource: EventSource,
            t: Throwable?,
            response: Response?
        ) {
            super.onFailure(eventSource, t, response)
            Log.e("MPC_${currentMcpConfig.name}", "Connection Failed with error: ${t?.message} response:${response?.message}")
        }

        override fun onClosed(eventSource: EventSource) {
            super.onClosed(eventSource)
            Log.d("MPC_${currentMcpConfig.name}", "Connection Closed")
        }
    }
}