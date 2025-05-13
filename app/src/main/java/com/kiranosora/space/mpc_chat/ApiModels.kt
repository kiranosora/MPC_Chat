package com.kiranosora.space.mpc_chat

data class ApiConfig(
    val name: String,       // 在下拉菜单中显示的名称
    val baseUrl: String,
    val modelName: String,
    val apiKey: String,
    val isOllama: Boolean=false
)

data class McpConfig(
    val name: String,       // 在下拉菜单中显示的名称
    val baseUrl: String,
    val apiKey: String
){
    companion object{
        const val DISABLE = "disable"
        const val LOCAL_MCP = "local mcp"
        const val DUMMY = "dummy"
        const val REMOTE_MCP = "remote mcp"
        const val REMOTE_BASE_URL = "https://kiranosora.space:11112/"
    }
}