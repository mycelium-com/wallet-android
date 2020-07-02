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

    fun orderPost(scope: CoroutineScope, symbol: String, side: String, quantity: String, clientOrderId: String,
                          type: String, timeInForce: String, price: String, stopPrice: String,
                          expireTime: java.util.Date, strictValidate: Boolean, postOnly: Boolean,
                          success: (Order?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.orderPost(symbol, side, quantity, clientOrderId, type, timeInForce, price, stopPrice,
                    expireTime, strictValidate, postOnly)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }
}