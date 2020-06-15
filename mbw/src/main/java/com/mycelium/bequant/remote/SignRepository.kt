package com.mycelium.bequant.remote

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
import com.mycelium.bequant.remote.client.apis.AccountApi
import com.mycelium.bequant.remote.client.apis.ApiKeyApi
import com.mycelium.bequant.remote.client.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class SignRepository {

    private val accountApi = AccountApi.create()
    private val apiKeyApi = ApiKeyApi.create()

    fun signUp(lifecycleOwner: LifecycleOwner,
               request: RegisterAccountRequest, success: () -> Unit) {
        doRequest(lifecycleOwner, {
            accountApi.postAccountRegister(request)
        }, {
            success.invoke()
        }, error = null)
    }


    fun authorize(lifecycleOwner: LifecycleOwner,
                  request: AccountAuthRequest, success: () -> Unit, error: (Int, String) -> Unit) {
        doRequest(lifecycleOwner, {
            accountApi.postAccountAuth(request)
        }, { response ->
            BequantPreference.setEmail(request.email)
            BequantPreference.setAccessToken(response?.accessToken ?: "")
            BequantPreference.setSession(response?.session ?: "")
            success.invoke()
        }, error = error)
    }

    fun resendRegister(lifecycleOwner: LifecycleOwner, request: AccountEmailConfirmResend) {
        doRequest(lifecycleOwner, {
            accountApi.postAccountEmailConfirmResend(request)
        }, {
        }, error = null)
    }

    fun totpCreate(lifecycleOwner: LifecycleOwner, success: (TotpCreateResponse?) -> Unit, error: (String) -> Unit) {
        doRequest(lifecycleOwner, {
            AccountApi.create().postAccountTotpCreate()
        }, {
            success.invoke(it)
        }, null)
    }

    fun totpActivate(lifecycleOwner: LifecycleOwner, totpActivateRequest: TotpActivateRequest, success: (SessionResponse?) -> Unit, error: (String) -> Unit) {
        doRequest(lifecycleOwner, {
            accountApi.postAccountTotpActivate(totpActivateRequest)
        }, {
            BequantPreference.setAccessToken(it?.accessToken ?: "")
            BequantPreference.setSession(it?.session ?: "")
            success.invoke(it)
        })
    }

    fun accountEmailConfirm(lifecycleOwner: LifecycleOwner, token: String, success: () -> Unit) {
        doRequest(lifecycleOwner, {
            accountApi.getAccountEmailConfirm(token)
        }, {
            success.invoke()
        })
    }

    fun accountTotpConfirm(lifecycleOwner: LifecycleOwner, token: String, success: () -> Unit) {
        doRequest(lifecycleOwner, {
            accountApi.getAccountTotpConfirm(token)
        }, {
            success.invoke()
        })
    }

    fun resetPassword(lifecycleOwner: LifecycleOwner, request: AccountPasswordResetRequest, success: () -> Unit, error: (String) -> Unit) {
        doRequest(lifecycleOwner, {
            AccountApi.create().postAccountPasswordReset(request)
        }, {}, null)
    }

    fun resetPasswordSet(lifecycleOwner: LifecycleOwner, request: AccountPasswordSetRequest, success: () -> Unit) {
        doRequest(lifecycleOwner, {
            AccountApi.create().postAccountPasswordSet(request)
        }, {
            success.invoke()
        })
    }

    fun getApiKeys(lifecycleOwner: LifecycleOwner, success: () -> Unit, error: (Int, String) -> Unit) {
        doRequest(lifecycleOwner, {
            apiKeyApi.postApiKey()
        }, {
            BequantPreference.setApiKeys(it?.privateKey, it?.publicKey)
            success.invoke()
        })
    }

    fun logout() {
        BequantPreference.clear()
    }

    companion object {
        val ENDPOINT = "https://xwpe71x4sg.execute-api.us-east-1.amazonaws.com/prd-reg/"
//        val ENDPOINT = "https://reg.bequant.io/"
//        val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJteWNlbGl1bSIsImp0aSI6ImJxN2g2M2ZzdmpvdG8xczVvaDEwIiwiaWF0IjoxNTg2NDM0ODI5LCJpc3MiOiJhdXRoLWFwaSIsImJpZCI6M30.0qvEnMxzbWF-P7eOpZDnSXwoOe5vDWluKFOFq5-tPaE"
        val repository by lazy { SignRepository() }
    }


    private fun <T> doRequest(lifecycleOwner: LifecycleOwner, request: suspend () -> Response<T>, invokeOnSuccess: (T?) -> Unit, error: ((Int, String) -> Unit)? = null) {
        if (lifecycleOwner is Fragment) {
            doRequest(lifecycleOwner.lifecycleScope, lifecycleOwner.parentFragmentManager, request, invokeOnSuccess, error)
            return
        }
        if (lifecycleOwner is AppCompatActivity) {
            doRequest(lifecycleOwner.lifecycleScope, lifecycleOwner.supportFragmentManager, request, invokeOnSuccess, error)
            return
        }

        throw NotImplementedError("$lifecycleOwner is not supported")
    }

    private fun <T> doRequest(lifecycleCoroutineScope: LifecycleCoroutineScope, fragmentManager: FragmentManager, request: suspend () -> Response<T>, invokeOnSuccess: (T?) -> Unit, error: ((Int, String) -> Unit)? = null) {
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


}