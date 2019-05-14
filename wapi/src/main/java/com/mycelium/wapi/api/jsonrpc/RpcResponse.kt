package com.mycelium.wapi.api.jsonrpc

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.io.BufferedReader

open class AbstractResponse

class RpcResponse : AbstractResponse() {
    companion object {
        fun fromJson(json: BufferedReader): RpcResponse = RPC.fromJson(json, RpcResponse::class.java)
    }

    @SerializedName(JSON_RPC_IDENTIFIER)
    val version: String? = null

    val id: Any = NO_ID
    val method: String? = null
    val error: RpcError? = null
    val result: JsonElement? = null
    val params: JsonElement? = null

    val isVoid: Boolean
        get() = hasResult && result == null

    val hasError: Boolean
        get() = error != null

    val hasResult: Boolean
        get() = !hasError && (result != null)

    val hasParams: Boolean
        get() = !hasError && (params != null)


    fun <T> getResult(clazz: Class<T>): T? {
        return if (hasResult) {
            Gson().fromJson(result, clazz)
        } else null
    }

    fun <T> getParams(clazz: Class<T>): T? {
        return if (hasParams) {
            Gson().fromJson(params, clazz)
        } else null
    }

    override fun toString() =
            "JsonRPCResponse{$JSON_RPC_IDENTIFIER=$version, $ID_KEY=$id, response=${(
                    if (hasError)
                        error.toString()
                    else
                        result.toString()
                    )}}"
}

class BatchedRpcResponse (responsessArr: Array<RpcResponse>): AbstractResponse() {
    val responses = responsessArr

    companion object {
        fun fromJson(json: BufferedReader) =
                BatchedRpcResponse(RPC.fromJson(json, Array<RpcResponse>::class.java))
    }
}
