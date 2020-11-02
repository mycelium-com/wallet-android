package com.mycelium.bequant.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

fun <T> doRequest(coroutineScope: CoroutineScope, request: suspend () -> Response<T>,
                  successBlock: (T?) -> Unit,
                  errorBlock: ((Int, String) -> Unit)? = null,
                  finallyBlock: (() -> Unit)? = null,
                  responseModifier: ((T?) -> T?)? = null) {
    coroutineScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val response = request()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        successBlock(responseModifier?.invoke(response.body()) ?: response.body())
                    } else {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        errorBlock?.invoke(response.code(), response.errorBody()?.string() ?: "")
                    }
                }
            } catch (e: Exception) {
                Log.w("Request", "exception on request", e)
                withContext(Dispatchers.Main) {
                    errorBlock?.invoke(400, e.message ?: "")
                }
            }
        }
    }.invokeOnCompletion {
        finallyBlock?.invoke()
    }
}
