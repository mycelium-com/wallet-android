package com.mycelium.wallet.external.changelly2.remote

import androidx.lifecycle.LifecycleCoroutineScope
import com.mycelium.bequant.remote.doRequest
import com.mycelium.wallet.external.changelly.ChangellyAPIService
import com.mycelium.wallet.external.changelly.model.*
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

object Changelly2Repository {
    private val api = ChangellyAPIService.retrofit.create(ChangellyAPIService::class.java)

    fun supportCurrencies(scope: CoroutineScope,
                          success: (ChangellyResponse<List<String>>?) -> Unit,
                          error: ((Int, String) -> Unit)? = null,
                          finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.currencies()
        }, success, error, finally)
    }

    fun supportCurrenciesFull(scope: CoroutineScope,
                              success: (ChangellyResponse<List<ChangellyCurrency>>?) -> Unit,
                              error: ((Int, String) -> Unit)? = null,
                              finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.currenciesFull()
        }, success, error, finally)
    }

    fun exchangeAmount(scope: CoroutineScope,
                       from: String,
                       to: String,
                       amount: BigDecimal,
                       success: (ChangellyResponse<FixRateForAmount>?) -> Unit,
                       error: (Int, String) -> Unit,
                       finally: (() -> Unit)? = null) =
            doRequest(scope, {
                api.exchangeAmountFix(exportSymbol(from), exportSymbol(to), amount)
            }, success, error, finally)

    fun fixRate(scope: CoroutineScope,
                from: String,
                to: String,
                success: (ChangellyResponse<FixRateForAmount>?) -> Unit,
                error: (Int, String) -> Unit,
                finally: (() -> Unit)? = null) =
            doRequest(scope, {
                api.exchangeAmountFix(exportSymbol(from), exportSymbol(to), BigDecimal.ONE)
            }, success, error, finally)

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
            api.createFixTransaction(exportSymbol(from), exportSymbol(to), amount, addressTo, rateId, refundAddress)
        }, success, error, finally)
    }

    fun getTransaction(scope: CoroutineScope,
                       id: String,
                       success: (ChangellyResponse<List<ChangellyTransaction>>?) -> Unit,
                       error: (Int, String) -> Unit,
                       finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.getTransaction(id)
        }, success, error, finally)
    }

    fun getTransactions(scope: LifecycleCoroutineScope, ids: List<String>,
                        success: (ChangellyResponse<List<ChangellyTransaction>>?) -> Unit,
                        error: (Int, String) -> Unit,
                        finally: (() -> Unit)? = null) {
        doRequest(scope, {
            api.getTransactions(ids)
        }, success, error, finally)
    }
}

fun ChangellyTransaction.fixedCurrencyFrom() =
        importSymbol(currencyFrom)

fun ChangellyTransaction.fixedCurrencyTo() =
        importSymbol(currencyTo)

private fun importSymbol(currency: String) =
        if (currency.equals("USDT20", true)) "USDT"
        else currency

private fun exportSymbol(currency: String) =
        if (currency.equals("USDT", true)) "USDT20".toLowerCase()
        else currency.toLowerCase()