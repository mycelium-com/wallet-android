package com.mycelium.bequant.remote.repositories

import com.mycelium.bequant.BequantConstants.EXCLUDE_COIN_LIST
import com.mycelium.bequant.BequantConstants.changeCoinToServer
import com.mycelium.bequant.remote.doRequest
import com.mycelium.bequant.remote.trading.api.AccountApi
import com.mycelium.bequant.remote.trading.model.*
import kotlinx.coroutines.CoroutineScope

class AccountApiRepository {
    private val api = AccountApi.create()

    fun accountCryptoWithdrawPost(scope: CoroutineScope,
                                  currency: String, amount: String, address: String, paymentId: String? = null, includeFee: Boolean? = null, autoCommit: Boolean? = null, useOffchain: String? = null,
                                  success: (InlineResponse200?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoWithdrawPost(currency, amount, address, paymentId, includeFee, autoCommit, useOffchain)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountBalanceGet(scope: CoroutineScope,
                          success: (Array<Balance>?) -> Unit, error: (Int, String) -> Unit, finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.accountBalanceGet()
        }, successBlock = success, errorBlock = error, finallyBlock = finally,
                responseModifier = { result ->
                    result?.filterNot { EXCLUDE_COIN_LIST.contains(it.currency) }?.apply {
                        firstOrNull { it.currency == "USD" }?.currency = "USDT"
                    }?.toTypedArray()
                })
    }

    fun cryptoAddressCurrencyGet(scope: CoroutineScope,
                                 currency: String,
                                 success: (Address?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoAddressCurrencyGet(changeCoinToServer(currency))
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun cryptoAddressCurrencyPost(scope: CoroutineScope,
                                  currency: String,
                                  success: (Address?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoAddressCurrencyPost(changeCoinToServer(currency))
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountCryptoCheckOffchainAvailablePost(scope: CoroutineScope,
                                                currency: String, address: String, paymentId: String,
                                                success: (WithdrawConfirm?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoCheckOffchainAvailablePost(currency, address, paymentId)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountCryptoIsMineAddressGet(scope: CoroutineScope,
                                      address: String,
                                      success: (AddressIsMineCheck?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoIsMineAddressGet(address)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountCryptoTransferConvertPost(scope: CoroutineScope,
                                         fromCurrency: String, toCurrency: String, amount: String,
                                         success: (Array<String>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoTransferConvertPost(fromCurrency, toCurrency, amount)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountTransactionsGet(scope: CoroutineScope,
                               currency: String, sort: String, by: String, from: String, till: String, limit: Int, offset: Int,
                               success: (Array<Transaction>?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountTransactionsGet(currency, sort, by, from, till, limit, offset)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountCryptoWithdrawIdDelete(scope: CoroutineScope,
                                      id: String,
                                      success: (WithdrawConfirm?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoWithdrawIdDelete(id)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountCryptoWithdrawIdPut(scope: CoroutineScope,
                                   id: String,
                                   success: (WithdrawConfirm?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountCryptoWithdrawIdPut(id)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountTransactionsIdGet(scope: CoroutineScope,
                                 id: String,
                                 success: (Transaction?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountTransactionsIdGet(id)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun accountTransferPost(scope: CoroutineScope,
                            currency: String, amount: String, type: String,
                            success: (InlineResponse200?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {
        doRequest(scope, {
            api.accountTransferPost(changeCoinToServer(currency), amount, type)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }
}
