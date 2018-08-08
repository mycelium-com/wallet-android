package com.mycelium.wapi.api.jsonrpc

class Subscription(val methodName: String, val params: RpcParams, val callback: Consumer<AbstractResponse>)