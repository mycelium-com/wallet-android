package com.mycelium.bequant.remote.repositories

import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.remote.client.apis.AccountApi
import com.mycelium.bequant.remote.client.apis.ApiKeyApi
import com.mycelium.bequant.remote.client.models.*
import com.mycelium.bequant.remote.doRequest
import kotlinx.coroutines.CoroutineScope

class SignRepository {
    private val accountApi = AccountApi.create()
    private val apiKeyApi = ApiKeyApi.create()

    fun signUp(scope: CoroutineScope,
               request: RegisterAccountRequest, success: (Unit?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountRegister(request)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun authorize(scope: CoroutineScope, request: AccountAuthRequest, success: (AccountAuthResponse?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountAuth(request)
        }, successBlock = { response ->
            BequantPreference.setEmail(request.email)
            BequantPreference.setAccessToken(response?.accessToken ?: "")
            BequantPreference.setSession(response?.session ?: "")
            success(response)
        }, errorBlock = error, finallyBlock = finally)
    }

    fun resendRegister(scope: CoroutineScope, request: AccountEmailConfirmResend, success: (Unit?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountEmailConfirmResend(request)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun totpCreate(scope: CoroutineScope, success: (TotpCreateResponse?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountTotpCreate(TotpCreateRequest())
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun totpActivate(scope: CoroutineScope, request: TotpActivateRequest, success: (SessionResponse?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountTotpActivate(request)
        }, {
            BequantPreference.setAccessToken(it?.accessToken ?: "")
            BequantPreference.setSession(it?.session ?: "")
            success(it)
        }, errorBlock = error, finallyBlock = finally)
    }

    fun accountEmailConfirm(scope: CoroutineScope, token: String, success: (Unit?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.getAccountEmailConfirm(token)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountOnceToken(scope: CoroutineScope, success: (OnceTokenResponse?) -> Unit, error: (Int, String) -> Unit, finally: (() -> Unit)?= null) {
        doRequest(scope, {
            accountApi.getAccountOnceToken()
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountTotpConfirm(scope: CoroutineScope, token: String, success: (Unit?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.getAccountTotpConfirm(token)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun resetPassword(scope: CoroutineScope, request: AccountPasswordResetRequest, success: (Unit?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountPasswordReset(request)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun resetPasswordSet(scope: CoroutineScope, request: AccountPasswordSetRequest, success: (Unit?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.postAccountPasswordSet(request)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getApiKeys(scope: CoroutineScope, success: (ApiKey?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            apiKeyApi.postApiKey(ApiKeyRequest())
        }, {
            BequantPreference.setApiKeys(it?.privateKey, it?.publicKey)
            success(it)
        }, errorBlock = error, finallyBlock = finally)
    }

    fun logout() {
        BequantPreference.clear()
    }
}