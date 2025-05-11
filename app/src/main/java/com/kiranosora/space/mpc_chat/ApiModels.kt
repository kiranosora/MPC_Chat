package com.kiranosora.space.mpc_chat

data class ApiConfig(
    val name: String,       // 在下拉菜单中显示的名称
    val baseUrl: String,
    val modelName: String,
    val apiKey: String,
    val isOllama: Boolean=false
)

data class MpcConfig(
    val name: String,       // 在下拉菜单中显示的名称
    val baseUrl: String,
    val apiKey: String
)