package com.kiranosora.space.mpc_chat.mpc
import com.google.gson.annotations.SerializedName

/**
 * Represents the root object of the chat request JSON.
 */
data class MpcChatRequest(
    @SerializedName("model")
    val model: String?,

    @SerializedName("messages")
    val messages: List<MpcChatMessage>?,

    @SerializedName("stream")
    val stream: Boolean?
)

/**
 * Represents a single message within the 'messages' list.
 */
data class MpcChatMessage(
    @SerializedName("role")
    val role: String?, // e.g., "system", "user"

    @SerializedName("content")
    val content: String? // The textual content of the message
)


/**
 * Represents a single function tool definition in the list.
 */
data class FunctionTool(
    @SerializedName("type")
    val type: String?, // Should typically be "function" based on the example

    @SerializedName("function")
    val functionDetails: FunctionDetails?
)

/**
 * Contains the details of the function itself.
 */
data class FunctionDetails(
    @SerializedName("name")
    val name: String?, // e.g., "add"

    @SerializedName("description")
    val description: String?, // e.g., "执行加法运算"

    @SerializedName("parameters")
    val parameters: FunctionParameters?
)

/**
 * Describes the parameters expected by the function.
 * Follows a structure similar to JSON Schema.
 */
data class FunctionParameters(
    @SerializedName("type")
    val type: String?, // Typically "object" for structured parameters

    // Uses a Map because the keys ("a", "b") are parameter names
    // and can vary depending on the function.
    @SerializedName("properties")
    val properties: Map<String, ParameterProperty>?,

    // A list of parameter names that are required.
    @SerializedName("required")
    val required: List<String>?
)

/**
 * Describes a single property (parameter) within the 'properties' map.
 */
data class ParameterProperty(
    @SerializedName("type")
    val type: String?, // e.g., "integer", "string", "number", "boolean"

    @SerializedName("description")
    val description: String? // Description of the parameter
)
