package com.mycelium.wallet.external.changelly2.remote

import androidx.lifecycle.LifecycleCoroutineScope
import com.mycelium.bequant.remote.doRequest
import com.mycelium.wallet.external.changelly.ChangellyRetrofitFactory
import com.mycelium.wallet.external.changelly.model.ChangellyCurrency
import com.mycelium.wallet.external.changelly.model.ChangellyListResponse
import com.mycelium.wallet.external.changelly.model.ChangellyResponse
import com.mycelium.wallet.external.changelly.model.ChangellyTransaction
import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer
import com.mycelium.wallet.external.changelly.model.FixRate
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal

object Changelly2Repository {
    private val api = ChangellyRetrofitFactory.api

    fun supportCurrenciesFull(
        scope: CoroutineScope,
        success: (ChangellyResponse<List<ChangellyCurrency>>?) -> Unit,
        error: ((Int, String) -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            api.getCurrenciesFull()
        }, success, error, finally)
    }

    fun getFixRateForAmount(
        scope: CoroutineScope,
        from: String,
        to: String,
        amount: BigDecimal,
        success: (ChangellyListResponse<FixRate>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) =
        doRequest(scope, {
            api.getFixRateForAmount(exportSymbol(from), exportSymbol(to), amount)
        }, success, error, finally)

    fun fixRate(
        scope: CoroutineScope,
        from: String,
        to: String,
        success: (ChangellyListResponse<FixRate>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) =
        doRequest(scope, {
            api.getFixRate(exportSymbol(from), exportSymbol(to))
        }, success, error, finally)

    fun createFixTransaction(
        scope: CoroutineScope,
        rateId: String,
        from: String,
        to: String,
        amount: String,
        addressTo: String,
        refundAddress: String,
        success: (ChangellyResponse<ChangellyTransactionOffer>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            api.createFixTransaction(
                exportSymbol(from),
                exportSymbol(to),
                amount,
                addressTo,
                rateId,
                refundAddress
            )
        }, success, error, finally)
    }

    fun getTransaction(
        scope: CoroutineScope,
        id: String,
        success: (ChangellyResponse<List<ChangellyTransaction>>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            api.getTransaction(id)
        }, success, error, finally)
    }

    fun getTransactions(
        scope: LifecycleCoroutineScope, ids: List<String>,
        success: (ChangellyResponse<List<ChangellyTransaction>>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
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
    if (currency.equals("USDT", true)) "USDT20"
    else currency