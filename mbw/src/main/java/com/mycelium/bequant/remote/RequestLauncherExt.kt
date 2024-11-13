package com.mycelium.bequant.remote

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

fun <T, R> doRequestModify(
    coroutineScope: CoroutineScope,
    request: suspend () -> Response<T>,
    successBlock: (R?) -> Unit,
    errorBlock: ((Int, String) -> Unit)? = null,
    finallyBlock: (() -> Unit)? = null,
    responseModifier: ((T?) -> R?)?
): Job =
    coroutineScope.launch {
        withContext(Dispatchers.IO) {
            try {
                val response = request()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        successBlock(responseModifier?.invoke(response.body()))
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
    }.apply {
        invokeOnCompletion {
            finallyBlock?.invoke()
        }
    }

fun <T> doRequest(
    coroutineScope: CoroutineScope,
    request: suspend () -> Response<T>,
    successBlock: (T?) -> Unit,
    errorBlock: ((Int, String) -> Unit)? = null,
    finallyBlock: (() -> Unit)? = null,
    responseModifier: ((T?) -> T?)? = { it }
): Job =
    doRequestModify(coroutineScope, request, successBlock, errorBlock, finallyBlock, responseModifier)