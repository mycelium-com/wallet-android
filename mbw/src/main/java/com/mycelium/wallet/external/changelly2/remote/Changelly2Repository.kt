package com.mycelium.wallet.external.changelly2.remote

import com.mycelium.bequant.remote.doRequest
import com.mycelium.wallet.external.changelly.ChangellyAPIService
import kotlinx.coroutines.CoroutineScope

object Changelly2Repository {
    private val api = ChangellyAPIService.retrofit.create(ChangellyAPIService::class.java)

    fun exchangeAmount(scope: CoroutineScope,
                       from: String,
                       to: String,
                       amount: Double,
                       success: (ChangellyAPIService.ChangellyAnswerDouble?) -> Unit,
                       error: (Int, String) -> Unit,
                       finally: () -> Unit) {
        doRequest(scope, {
            api.exchangeAmount(from, to, amount)
        }, success, error, finally)
    }
}