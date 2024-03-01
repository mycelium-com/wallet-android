package com.mycelium.wallet.external

data class DefaultJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String = "test",
    val method: String? = null,
    val params: Map<String, String>? = null,
)
