package com.mycelium.bequant.remote

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response


fun <T> LifecycleOwner.doRequest(request: suspend () -> Response<T>, successBlock: (T?) -> Unit, errorBlock: (Int, String) -> Unit, finallyBlock: () -> Unit) {
    doRequest(lifecycleScope, request, successBlock, errorBlock, finallyBlock)
}

fun <T> doRequest(lifecycleCoroutineScope: LifecycleCoroutineScope, request: suspend () -> Response<T>, successBlock: (T?) -> Unit, errorBlock: (Int, String) -> Unit, finallyBlock: () -> Unit) {
    lifecycleCoroutineScope.launch {
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
