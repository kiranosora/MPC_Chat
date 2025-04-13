package com.kiranosora.space.mpc_chat.mpc

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.kiranosora.space.mpc_chat.mpc.MpcRequestHelper.Companion.gson

/**
 * Root object for the OpenAPI specification.
 */
data class MpcInfoResponse(
    @SerializedName("openapi") val openapi: String?,
    @SerializedName("info") val info: Info?,
    @SerializedName("paths") val paths: Map<String, PathItem>?,
    @SerializedName("components") val components: Components?
){
    companion object{
        val MIME_JSON = "application/json"
    }
    fun toFunctionTool(): List<FunctionTool> {
        val ret = mutableListOf<FunctionTool>() // Use a mutable list
        if (paths != null) {
            for ((pathKey, pathItem) in paths) {
                // Iterate all the operations
                val toolName = pathKey.substring(1)
                val schemas = this.components?.schemas
                val parameters = this.components?.schemas?.get("${toolName}_form_model")?.properties
                val properties = mutableMapOf<String, ParameterProperty>()
                Log.d("toFunctionTool", "properties: $properties parameters: $parameters toolName: $toolName, schemas: ${schemas?.keys}")
                if (parameters != null) {
                    for((paraName, paraItem) in parameters){
                        val paraProp = ParameterProperty(paraItem.type, paraItem.description)
                        properties.put(paraName, paraProp)
                    }
                }
                val required = this.components?.schemas?.get(toolName)?.required
                if(pathItem.post != null){
                        val functionTool = FunctionTool(
                            "function",
                            FunctionDetails(
                                pathKey,
                                pathItem.post.description,
                                FunctionParameters("object", properties, required) // You'll need to build these based on your Schema
                            )
                        )
                        ret.add(functionTool)
                }
            }
        }

        return ret
    }
}

/**
 * General information about the API.
 */
data class Info(
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("version") val version: String?
)

/**
 * Holds the available operations for a single path.
 * Keys in the `paths` map point to this object.
 */
data class PathItem(
    // Only 'post' is present in the example, add others (get, put, delete, etc.) if needed
    @SerializedName("post") val post: Operation?
)

/**
 * Describes a single API operation on a path.
 */
data class Operation(
    @SerializedName("summary") val summary: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("operationId") val operationId: String?,
    @SerializedName("requestBody") val requestBody: RequestBody?,
    @SerializedName("responses") val responses: Map<String, Response>? // Key is HTTP status code (e.g., "200", "422")
)

/**
 * Describes the request body of the operation.
 */
data class RequestBody(
    @SerializedName("content") val content: Map<String, MediaType>?, // Key is content type (e.g., "application/json")
    @SerializedName("required") val required: Boolean?
)

/**
 * Describes the schema and examples for a specific media type.
 */
data class MediaType(
    @SerializedName("schema") val schema: Schema?
)

/**
 * Represents a schema object, often referencing another schema.
 */
data class Schema(
    // Using @SerializedName because '$' is not valid in Kotlin identifiers
    @SerializedName("\$ref") val ref: String?
    // Add other schema properties (type, properties, items, etc.) if schemas are defined inline
)

/**
 * Describes a single response from an API Operation.
 * Keys in the `responses` map point to this object.
 */
data class Response(
    @SerializedName("description") val description: String?,
    @SerializedName("content") val content: Map<String, MediaType>? // Key is content type (e.g., "application/json")
)

/**
 * Holds reusable components for the OpenAPI specification.
 */
data class Components(
    @SerializedName("schemas") val schemas: Map<String, SchemaDefinition>? // Key is the schema name (e.g., "HTTPValidationError")
)

/**
 * Defines a schema structure used in the API.
 */
data class SchemaDefinition(
    @SerializedName("properties") val properties: Map<String, PropertyDefinition>?, // Key is property name
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("required") val required: List<String>?,
    // Added for array types like in HTTPValidationError.detail.items
    @SerializedName("items") val items: ItemDefinition?
)

/**
 * Defines a property within a schema.
 */
data class PropertyDefinition(
    // For array properties
    @SerializedName("items") val items: ItemDefinition?,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    // For properties that can be one of several types (like in ValidationError.loc.items)
    @SerializedName("anyOf") val anyOf: List<AnyOfItem>?
)

/**
 * Defines the items structure within an array property or schema.
 */
data class ItemDefinition(
    // If items refer to another schema
    @SerializedName("\$ref") val ref: String?,
    // If items can be one of several types (like string or integer)
    @SerializedName("anyOf") val anyOf: List<AnyOfItem>?
    // Add 'type' if items have a direct type (e.g., "type": "string")
)

/**
 * Represents one of the possible types in an 'anyOf' list.
 */
data class AnyOfItem(
    @SerializedName("type") val type: String?
)

// --- Specific Schema Definitions (Optional but can be useful for clarity) ---
// You might not need separate classes for every schema defined under components/schemas
// if SchemaDefinition is flexible enough, but defining them explicitly can sometimes
// improve type safety if you intend to work directly with specific schema types after
// deserialization. Here are examples based on your JSON:

data class HTTPValidationError(
    @SerializedName("detail") val detail: List<ValidationError>?
)

data class ValidationError(
    // Note: 'loc' items can be string or integer, Gson might handle List<Any> or you might need a custom TypeAdapter
    @SerializedName("loc") val loc: List<Any>?, // Using Any as a placeholder
    @SerializedName("msg") val msg: String?,
    @SerializedName("type") val type: String?
)

data class AddFormModel(
    @SerializedName("a") val a: Int?,
    @SerializedName("b") val b: Int?
)

data class CalculatorFormModel(
    @SerializedName("representation") val representation: String?
)

// Empty object models can just be represented by an empty data class or even `Any`
data class GetCurrentLocalTimeFormModel(val ignored: Any? = null) // Or just use Any/Object
data class GetMemoryFormModel(val ignored: Any? = null)
data class ListDownloadedModelsFormModel(val ignored: Any? = null)
data class ListInferenceModelFormModel(val ignored: Any? = null)

data class LoadModelFormModel(
    @SerializedName("model_path") val modelPath: String?
)

data class ScrapeWebContentFromUrlFormModel(
    @SerializedName("url") val url: String?
)

data class UnloadModelFormModel(
    @SerializedName("model_path") val modelPath: String?
)


/**
 * Represents an element within the main JSON array.
 * Often used in streaming responses.
 */
/**
 * 主响应对象（最外层容器）
 */
data class ToolChatCompletionChunk(
    @SerializedName("id")
    val id: String?,

    @SerializedName("object")
    val objectType: String?, // 使用 object 后缀避免与 Kotlin 关键字冲突

    @SerializedName("created")
    val createdTime: Long?,

    @SerializedName("model")
    val model: String?,

    @SerializedName("system_fingerprint")
    val systemFingerprint: String?,

    @SerializedName("choices")
    val choices: List<Choice>?
)

/**
 * 表示选择项（choices 数组中的元素）
 */
data class Choice(
    @SerializedName("index")
    val index: Int?,

    @SerializedName("delta")
    val delta: Delta?,

    @SerializedName("logprobs")
    val logProbs: Any?, // 可能为 null 或复杂结构，暂用Any类型

    @SerializedName("finish_reason")
    val finishReason: String?
)
/**
 * Represents the content of the "delta" object.
 */
data class Delta(
    // Maps the JSON key "tool_calls" (snake_case)
    // to the Kotlin property 'toolCalls' (camelCase)
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCallChunk>?
    // Add other possible fields in delta if they exist, e.g., "content": String?
)

/**
 * Represents an element within the "tool_calls" array inside the delta.
 */
data class ToolCallChunk(
    @SerializedName("index")
    val index: Int?,

    @SerializedName("id")
    val id: String?,

    @SerializedName("type")
    val type: String?, // e.g., "function"

    @SerializedName("function")
    val function: FunctionCallArguments?
)

/**
 * Represents the "function" object within a tool call chunk.
 * Contains the function name and potentially partial arguments.
 */
data class FunctionCallArguments(
    @SerializedName("name")
    var name: String?,

    @SerializedName("arguments")
    var arguments: String? // Note: Arguments often arrive fragmented in streams
)