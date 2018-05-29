package com.mycelium.wapi.api.jsonrpc

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

class RpcResponse {

    companion object {

        fun fromJson(json: String): RpcResponse {
            return RPC.fromJson(json, RpcResponse::class.java)
        }

    }

    @SerializedName(JSON_RPC_IDENTIFIER)
    val version: String? = null

    val id: Any = NO_ID
    val error: RpcError? = null
    private val result: JsonElement? = null

    val isVoid: Boolean
        get() = hasResult && result == null

    val hasError: Boolean
        get() = error != null

    val hasResult: Boolean
        get() = !hasError

    inline fun <reified T> getResult(): T? {
        return getResult(T::class.java)
    }

    fun <T> getResult(clazz: Class<T>): T? {
        return if (hasResult) {
            Gson().fromJson(result, clazz)
        } else null
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun toString(): String {
        return "JsonRPCResponse{" + "$JSON_RPC_IDENTIFIER=$version, $ID_KEY=$id, response=${(if (hasError) error else result)}}"
    }
}