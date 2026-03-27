package com.emanueledipietro.remodex.data.connection

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class RpcMessage(
    val jsonrpc: String? = null,
    val id: JsonElement? = null,
    val method: String? = null,
    val params: JsonElement? = null,
    val result: JsonElement? = null,
    val error: RpcError? = null,
) {
    companion object {
        fun request(
            id: JsonElement,
            method: String,
            params: JsonElement? = null,
        ): RpcMessage {
            return RpcMessage(
                jsonrpc = null,
                id = id,
                method = method,
                params = params,
            )
        }

        fun notification(
            method: String,
            params: JsonElement? = null,
        ): RpcMessage {
            return RpcMessage(
                jsonrpc = null,
                method = method,
                params = params,
            )
        }

        fun response(
            id: JsonElement?,
            result: JsonElement,
        ): RpcMessage {
            return RpcMessage(
                jsonrpc = null,
                id = id,
                result = result,
            )
        }

        fun error(
            id: JsonElement?,
            code: Int,
            message: String,
            data: JsonElement? = null,
        ): RpcMessage {
            return RpcMessage(
                jsonrpc = null,
                id = id,
                error = RpcError(
                    code = code,
                    message = message,
                    data = data,
                ),
            )
        }
    }

    val isRequest: Boolean
        get() = method != null

    val isResponse: Boolean
        get() = result != null || error != null
}

@Serializable
data class RpcError(
    val code: Int,
    override val message: String,
    val data: JsonElement? = null,
) : Exception(message)

fun rpcIdKey(id: JsonElement?): String? {
    return when (id) {
        null,
        JsonNull -> null

        is JsonPrimitive -> id.contentOrNull ?: id.toString()
        else -> id.toString()
    }?.trim()?.ifEmpty { null }
}

val JsonElement?.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

val JsonElement?.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

val JsonElement?.stringOrNull: String?
    get() = (this as? JsonPrimitive)?.contentOrNull?.trim()?.ifEmpty { null }

val JsonElement?.rawStringOrNull: String?
    get() = (this as? JsonPrimitive)?.contentOrNull

val JsonElement?.intOrNull: Int?
    get() = (this as? JsonPrimitive)?.intOrNull

val JsonElement?.longOrNull: Long?
    get() = (this as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

val JsonElement?.doubleOrNull: Double?
    get() = (this as? JsonPrimitive)?.doubleOrNull

val JsonElement?.booleanOrNull: Boolean?
    get() = (this as? JsonPrimitive)?.booleanOrNull

fun JsonObject.firstValue(vararg keys: String): JsonElement? {
    return keys.asSequence()
        .mapNotNull(::get)
        .firstOrNull()
}

fun JsonObject.firstString(vararg keys: String): String? = firstValue(*keys).stringOrNull

fun JsonObject.firstRawString(vararg keys: String): String? = firstValue(*keys).rawStringOrNull

fun JsonObject.firstInt(vararg keys: String): Int? = firstValue(*keys).intOrNull

fun JsonObject.firstLong(vararg keys: String): Long? = firstValue(*keys).longOrNull

fun JsonObject.firstDouble(vararg keys: String): Double? = firstValue(*keys).doubleOrNull

fun JsonObject.firstBoolean(vararg keys: String): Boolean? = firstValue(*keys).booleanOrNull

fun JsonObject.firstObject(vararg keys: String): JsonObject? = firstValue(*keys).jsonObjectOrNull

fun JsonObject.firstArray(vararg keys: String): JsonArray? = firstValue(*keys).jsonArrayOrNull
