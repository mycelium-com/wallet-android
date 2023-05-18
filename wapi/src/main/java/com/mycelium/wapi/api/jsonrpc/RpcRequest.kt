package com.mycelium.wapi.api.jsonrpc

import com.google.gson.annotations.SerializedName
import kotlin.random.Random


class RpcRequestOut(
        @SerializedName(METHOD_KEY)
        val methodName: String,
        val params: RpcParams = RpcNoParams
) {
    var id: String = Random.nextInt().toString()

    @SerializedName(JSON_RPC_IDENTIFIER)
    var version = JSON_RPC_VERSION

    fun toJson(): String = RPC.toJson(this)
}
