package com.mycelium.wapi.api.jsonrpc

import com.google.gson.*
import java.lang.reflect.Type

sealed class RpcParams {
    abstract val paramCount: Int

    companion object {
        @JvmStatic
        fun noParams() = RpcNoParams

        @JvmStatic
        fun listParams(value: List<Any>) = RpcListParams(value)

        @JvmStatic
        fun listParams(vararg value: Any) = RpcListParams(value.toList())

        @JvmStatic
        fun mapParams(value: Map<String, Any>) = RpcMapParams(value)

        @JvmStatic
        fun mapParams(vararg value: Pair<String, Any>) = RpcMapParams(value.toMap())
    }
}

object RpcNoParams : RpcParams() {
    override val paramCount: Int
        get() = 0
}

class RpcListParams<out E>(
        val value: List<E>
) : RpcParams() {
    constructor(vararg value: E) : this(value.toList())

    override val paramCount: Int
        get() = value.size

    override fun toString(): String = "RpcListParams(value=$value)"
}

class RpcMapParams<out E>(
        val value: Map<String, E>
) : RpcParams() {
    override val paramCount: Int
        get() = value.size

    override fun toString(): String = "RpcMapParams(value=$value)"
}

class RpcParamsTypeAdapter : JsonDeserializer<RpcParams>, JsonSerializer<RpcParams> {
    override fun serialize(src: RpcParams, typeOfSrc: Type, context: JsonSerializationContext): JsonElement? {
        return when (src) {
            RpcNoParams -> null
            is RpcListParams<*> -> RPC.toJsonTree(src.value)
            is RpcMapParams<*> -> RPC.toJsonTree(src.value)
        }
    }

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): RpcParams {
        return when {
            json.isJsonArray -> RpcListParams<JsonElement>(json.asJsonArray.toMutableList())
            json.isJsonObject -> RpcMapParams<JsonElement>(json.asJsonObject.entrySet().associate { it.key to it.value })
            else -> RpcNoParams
        }
    }
}
