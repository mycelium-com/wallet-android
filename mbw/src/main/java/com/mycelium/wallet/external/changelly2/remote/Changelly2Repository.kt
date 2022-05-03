package com.mycelium.wallet.external.changelly2.remote

import com.mycelium.bequant.remote.doRequest
import com.mycelium.wallet.external.changelly.ChangellyAPIService
import com.mycelium.wallet.external.changelly.model.ChangellyResponse
import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer
import com.mycelium.wallet.external.changelly.model.FixRate
import com.mycelium.wallet.external.changelly.model.FixRateForAmount
import kotlinx.coroutines.CoroutineScope

object Changelly2Repository {
    private val api = ChangellyAPIService.retrofit.create(ChangellyAPIService::class.java)

    fun exchangeAmount(scope: CoroutineScope,
                       from: String,
                       to: String,
                       amount: Double,
                       success: (ChangellyResponse<FixRateForAmount>?) -> Unit,
                       error: (Int, String) -> Unit,
                       finally: () -> Unit) {
        doRequest(scope, {
            api.exchangeAmountFix(from, to, amount)
        }, success, error, finally)
    }

    fun fixRate(scope: CoroutineScope,
                from: String,
                to: String,
                success: (ChangellyResponse<FixRate>?) -> Unit,
                error: (Int, String) -> Unit,
                finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.fixRate(from, to)
        }, success, error, finally)
    }

    fun createFixTransaction(scope: CoroutineScope,
                             rateId: String,
                             from: String,
                             to: String,
                             amount: String,
                             addressTo: String,
                             refundAddress: String,
                             success: (ChangellyResponse<ChangellyTransactionOffer>?) -> Unit,
                             error: (Int, String) -> Unit,
                             finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.createFixTransaction(from, to, amount, addressTo, rateId, refundAddress)
        }, success, error, finally)
    }
}