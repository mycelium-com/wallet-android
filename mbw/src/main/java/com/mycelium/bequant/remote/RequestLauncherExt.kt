package com.mycelium.bequant.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

fun <T> doRequest(coroutineScope: CoroutineScope, request: suspend () -> Response<T>, successBlock: (T?) -> Unit, errorBlock: (Int, String) -> Unit, finallyBlock: () -> Unit) {
    coroutineScope.launch {
        withContext(Dispatchers.IO) {
            val response = request()
            if (response.isSuccessful) {
                successBlock(response.body())
            } else {
                errorBlock.invoke(response.code(), response.errorBody()?.string() ?: "")
            }
        }
    }
            .invokeOnCompletion {
                finallyBlock.invoke()
            }
}
