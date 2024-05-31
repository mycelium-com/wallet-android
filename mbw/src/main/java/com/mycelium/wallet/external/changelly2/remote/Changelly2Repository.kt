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
import retrofit2.HttpException
import java.math.BigDecimal

object Changelly2Repository {
    private val userRepository by lazy { Api.statusRepository }
    private val viperApi by lazy { ChangellyRetrofitFactory.viperApi }
    private val changellyApi = ChangellyRetrofitFactory.changellyApi

    fun supportCurrenciesFull(
        scope: CoroutineScope,
        success: (ChangellyResponse<List<ChangellyCurrency>>?) -> Unit,
        error: ((Int, String) -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            changellyApi.getCurrenciesFull()
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
            val isVip = userRepository.statusFlow.value.isVIP()
            val api = if (isVip) viperApi else changellyApi
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
            changellyApi.getFixRate(exportSymbol(from), exportSymbol(to))
        }, success, error, finally)

    suspend fun createFixTransaction(
        rateId: String,
        from: String,
        to: String,
        amount: String,
        addressTo: String,
        refundAddress: String,
        changellyOnly: Boolean,
    ): ChangellyResponse<ChangellyTransactionOffer> {
        val isVip = userRepository.statusFlow.value.isVIP()
        val fromSymbol = exportSymbol(from)
        val toSymbol = exportSymbol(to)
        if (!isVip || changellyOnly) {
            return changellyApi.createFixTransaction(
                fromSymbol,
                toSymbol,
                amount,
                addressTo,
                rateId,
                refundAddress,
            )
        }
        try {
            return viperApi.createFixTransaction(
                fromSymbol,
                toSymbol,
                amount,
                addressTo,
                rateId,
                refundAddress,
            )
        } catch (e: Exception) {
            // Http exception with 401 unauthorized code means that user isn't vip anymore
            if (e is HttpException && e.code() == 401) {
                userRepository.dropStatus()
                throw ViperStatusException(e)
            }
            throw ViperUnexpectedException(e)
        }
    }

    fun getTransaction(
        scope: CoroutineScope,
        id: String,
        success: (ChangellyResponse<List<ChangellyTransaction>>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            changellyApi.getTransaction(id)
        }, success, error, finally)
    }

    fun getTransactions(
        scope: LifecycleCoroutineScope, ids: List<String>,
        success: (ChangellyResponse<List<ChangellyTransaction>>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            changellyApi.getTransactions(ids)
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

class ViperUnexpectedException(e: Exception) : Exception(e)
class ViperStatusException(e: Exception) : Exception(e)