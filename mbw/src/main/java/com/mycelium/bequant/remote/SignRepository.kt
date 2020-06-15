package com.mycelium.bequant.remote

import androidx.lifecycle.LifecycleOwner
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.remote.client.apis.AccountApi
import com.mycelium.bequant.remote.client.apis.ApiKeyApi
import com.mycelium.bequant.remote.client.models.*

class SignRepository {

    private val accountApi = AccountApi.create()
    private val apiKeyApi = ApiKeyApi.create()

    fun signUp(lifecycleOwner: LifecycleOwner,
               request: RegisterAccountRequest, success: () -> Unit) {
        lifecycleOwner.doRequest({
            accountApi.postAccountRegister(request)
        }, {
            success.invoke()
        }, error = null)
    }


    fun authorize(lifecycleOwner: LifecycleOwner,
                  request: AccountAuthRequest, success: () -> Unit, error: (Int, String) -> Unit) {
        lifecycleOwner.doRequest({
            accountApi.postAccountAuth(request)
        }, { response ->
            BequantPreference.setEmail(request.email)
            BequantPreference.setAccessToken(response?.accessToken ?: "")
            BequantPreference.setSession(response?.session ?: "")
            success.invoke()
        }, error = error)
    }

    fun resendRegister(lifecycleOwner: LifecycleOwner, request: AccountEmailConfirmResend) {
        lifecycleOwner.doRequest({
            accountApi.postAccountEmailConfirmResend(request)
        }, {
        }, error = null)
    }

    fun totpCreate(lifecycleOwner: LifecycleOwner, success: (TotpCreateResponse?) -> Unit, error: (String) -> Unit) {
        lifecycleOwner.doRequest({
            AccountApi.create().postAccountTotpCreate()
        }, {
            success.invoke(it)
        }, null)
    }

    fun totpActivate(lifecycleOwner: LifecycleOwner, totpActivateRequest: TotpActivateRequest, success: (SessionResponse?) -> Unit, error: (String) -> Unit) {
        lifecycleOwner.doRequest({
            accountApi.postAccountTotpActivate(totpActivateRequest)
        }, {
            BequantPreference.setAccessToken(it?.accessToken ?: "")
            BequantPreference.setSession(it?.session ?: "")
            success.invoke(it)
        })
    }

    fun accountEmailConfirm(lifecycleOwner: LifecycleOwner, token: String, success: () -> Unit) {
        lifecycleOwner.doRequest({
            accountApi.getAccountEmailConfirm(token)
        }, {
            success.invoke()
        })
    }

    fun accountTotpConfirm(lifecycleOwner: LifecycleOwner, token: String, success: () -> Unit) {
        lifecycleOwner.doRequest({
            accountApi.getAccountTotpConfirm(token)
        }, {
            success.invoke()
        })
    }

    fun resetPassword(lifecycleOwner: LifecycleOwner, request: AccountPasswordResetRequest, success: () -> Unit, error: (String) -> Unit) {
        lifecycleOwner.doRequest({
            AccountApi.create().postAccountPasswordReset(request)
        }, {}, null)
    }

    fun resetPasswordSet(lifecycleOwner: LifecycleOwner, request: AccountPasswordSetRequest, success: () -> Unit) {
        lifecycleOwner.doRequest({
            AccountApi.create().postAccountPasswordSet(request)
        }, {
            success.invoke()
        })
    }

    fun getApiKeys(lifecycleOwner: LifecycleOwner, success: () -> Unit, error: (Int, String) -> Unit) {
        lifecycleOwner.doRequest({
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

}