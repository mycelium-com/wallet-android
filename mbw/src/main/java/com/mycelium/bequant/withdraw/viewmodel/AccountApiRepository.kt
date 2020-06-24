package com.mycelium.bequant.withdraw.viewmodel

import com.mycelium.bequant.remote.doRequest
import com.mycelium.bequant.remote.trading.api.AccountApi
import com.mycelium.bequant.remote.trading.model.InlineResponse200
import kotlinx.coroutines.CoroutineScope

class AccountApiRepository {

    private val accountApi = AccountApi.create()

    fun withdraw(scope: CoroutineScope,
                 currency: kotlin.String, amount: kotlin.String, address: kotlin.String, paymentId: kotlin.String, includeFee: kotlin.Boolean, autoCommit: kotlin.Boolean, useOffchain: kotlin.String,
                 success: (InlineResponse200?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            accountApi.accountCryptoWithdrawPost(currency, amount, address, paymentId, includeFee, autoCommit, useOffchain)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

}
