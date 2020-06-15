package com.mycelium.bequant.remote

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response


fun <T> LifecycleOwner.doRequest(request: suspend () -> Response<T>, invokeOnSuccess: (T?) -> Unit, error: ((Int, String) -> Unit)? = null) {
    if (this is Fragment) {
        doRequest(lifecycleScope, this.parentFragmentManager, request, invokeOnSuccess, error)
        return
    }
    if (this is AppCompatActivity) {
        doRequest(lifecycleScope, this.supportFragmentManager, request, invokeOnSuccess, error)
        return
    }

    throw NotImplementedError("$this is not supported")
}

fun <T> doRequest(lifecycleCoroutineScope: LifecycleCoroutineScope, fragmentManager: FragmentManager, request: suspend () -> Response<T>, invokeOnSuccess: (T?) -> Unit, error: ((Int, String) -> Unit)? = null) {
    val loader = LoaderFragment()
    lifecycleCoroutineScope.launch {
        loader.show(fragmentManager, "loader")
        withContext(Dispatchers.IO) {
            val response = request()
            if (response.isSuccessful) {
                invokeOnSuccess(response.body())
            } else {
                error?.invoke(response.code(), response.errorBody()?.string() ?: "")
            }
        }
    }
            .invokeOnCompletion {
                loader.dismissAllowingStateLoss()
                it?.let { ErrorHandler(loader.requireContext()).handle(it.message ?: "Error") }
            }
}
