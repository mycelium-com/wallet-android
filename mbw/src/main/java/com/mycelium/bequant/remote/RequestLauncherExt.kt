package com.mycelium.bequant.remote

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

fun <T> doRequest(
    coroutineScope: CoroutineScope, request: suspend () -> Response<T>,
    successBlock: (T?) -> Unit,
    errorBlock: ((Int, String) -> Unit)? = null,
    finallyBlock: (() -> Unit)? = null,
    responseModifier: ((T?) -> T?)? = null
) {
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


fun <T> doRequest(
    request: suspend () -> Response<T>
): Flow<Event<T>> {
    return flow {
        emit(Event.loading<T>())
        val response = request()
        if (response.isSuccessful) {
            emit(Event.success(response.body()))
        } else {
            @Suppress("BlockingMethodInNonBlockingContext")
            emit(Event.error<T>(Error(response.errorBody()?.string() ?: "")))
        }
    }.catch { error ->
        emit(Event.error<T>(Error(error)))
    }.flowOn(Dispatchers.IO)

}

data class Event<out T>(val status: Status, val data: T?, val error: Error?) {

    companion object {
        fun <T> loading(): Event<T> {
            return Event(Status.LOADING, null, null)
        }

        fun <T> success(data: T?): Event<T> {
            return Event(Status.SUCCESS, data, null)
        }

        fun <T> error(error: Error?): Event<T> {
            return Event(Status.ERROR, null, error)
        }
    }
}

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}