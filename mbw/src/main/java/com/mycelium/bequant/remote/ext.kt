package com.mycelium.bequant.remote

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

fun <T> Fragment.load(request: suspend () -> Response<T>, invokeOnSuccess: (T?) -> Unit) {
    val loader = LoaderFragment()
    loader.show(parentFragmentManager, "loader")
    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            val postAccountRegister = request()
            if (postAccountRegister.isSuccessful) {
                invokeOnSuccess(postAccountRegister.body())
            }
        }
    }.invokeOnCompletion {
        loader.dismissAllowingStateLoss()
        it?.let { ErrorHandler(requireContext()).handle(it.message ?: "Error") }
    }
}

fun <T> AppCompatActivity.load(request: suspend () -> Response<T>, invokeOnSuccess: (T?) -> Unit) {
    val loader = LoaderFragment()
    loader.show(supportFragmentManager, "loader")
    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            val postAccountRegister = request()
            if (postAccountRegister.isSuccessful) {
                invokeOnSuccess(postAccountRegister.body())
            }
        }
    }.invokeOnCompletion {
        loader.dismissAllowingStateLoss()
        it?.let { ErrorHandler(this).handle(it.message ?: "Error") }
    }
}