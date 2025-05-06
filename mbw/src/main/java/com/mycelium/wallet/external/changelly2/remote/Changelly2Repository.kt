package com.mycelium.wallet.external.changelly2.remote

import com.mycelium.bequant.remote.doRequest
import com.mycelium.wallet.external.changelly.ChangellyRetrofitFactory
import com.mycelium.wallet.external.changelly.model.ChangellyCurrency
import com.mycelium.wallet.external.changelly.model.ChangellyListResponse
import com.mycelium.wallet.external.changelly.model.ChangellyResponse
import com.mycelium.wallet.external.changelly.model.ChangellyTransaction
import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer
import com.mycelium.wallet.external.changelly.model.Error
import com.mycelium.wallet.external.changelly.model.FixRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
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

    suspend fun getTransaction(id: String): ChangellyResponse<List<ChangellyTransaction>> {
        val isVip = userRepository.statusFlow.value.isVIP()
        val changellyTransactions = changellyApi.getTransaction(id)
        if (!isVip) return changellyTransactions
        if (changellyTransactions.result?.any { it.id == id } == true) return changellyTransactions
        return try {
            viperApi.getTransaction(id)
        } catch (e: HttpException) {
            ChangellyResponse(null, Error(e.code(), e.message()))
        } catch (e: Exception) {
            ChangellyResponse(null, Error(500, e.message ?: ""))
        }
    }

    suspend fun getTransactions(ids: List<String>): ChangellyResponse<List<ChangellyTransaction>> {
        val isVip = userRepository.statusFlow.value.isVIP()
        if (!isVip) return changellyApi.getTransactions(ids)
        val changellyTransactionsDeferred = withContext(Dispatchers.IO) {
            async { changellyApi.getTransactions(ids) }
        }
        val viperTransactionsDeferred = withContext(Dispatchers.IO) {
            async {
                try {
                    viperApi.getTransactions(ids)
                } catch (e: HttpException) {
                    ChangellyResponse(null, Error(e.code(), e.message()))
                } catch (e: Exception) {
                    ChangellyResponse(null, Error(500, e.message ?: ""))
                }
            }
        }
        val changellyTransactions = changellyTransactionsDeferred.await()
        val viperTransactions = viperTransactionsDeferred.await()
        val changellyResult = changellyTransactions.result ?: emptyList()
        val viperResult = viperTransactions.result ?: emptyList()
        return ChangellyResponse(changellyResult + viperResult)
    }
}

fun ChangellyTransaction.fixedCurrencyFrom() =
    importSymbol(currencyFrom)

fun ChangellyTransaction.fixedCurrencyTo() =
    importSymbol(currencyTo)

fun importSymbol(currency: String) =
    if (currency.equals("USDT", true)) "USDT20"
    else currency

private fun exportSymbol(currency: String) = currency
//    if (currency.equals("USDT20", true) && isVip) "usdt"
//    else currency.lowercase()

class ViperUnexpectedException(e: Exception) : Exception(e)
class ViperStatusException(e: Exception) : Exception(e)