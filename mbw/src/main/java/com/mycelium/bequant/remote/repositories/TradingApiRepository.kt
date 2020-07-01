package com.mycelium.bequant.remote.repositories

import com.mycelium.bequant.remote.doRequest
import com.mycelium.bequant.remote.trading.api.TradingApi
import com.mycelium.bequant.remote.trading.model.*
import kotlinx.coroutines.CoroutineScope

class TradingApiRepository {
    private val api = TradingApi.create()

    fun tradingBalanceGet(scope: CoroutineScope,
                          success: (Array<Balance>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.tradingBalanceGet()
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }
}