package com.mycelium.wallet.external.changelly

import com.mycelium.wallet.external.changelly.model.ChangellyCurrency
import com.mycelium.wallet.external.changelly.model.ChangellyGetExchangeAmountResponse
import com.mycelium.wallet.external.changelly.model.ChangellyListResponse
import com.mycelium.wallet.external.changelly.model.ChangellyResponse
import com.mycelium.wallet.external.changelly.model.ChangellyTransaction
import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer
import com.mycelium.wallet.external.changelly.model.FixRate
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal

/**
 * Interface to describing Changelly API for retrofit2 library and providing retrofit object intialization.
 */
interface ChangellyAPIService {

    // end data classes
    @POST("getCurrencies")
    fun getCurrencies(): Call<ChangellyResponse<List<String>>>

    // {"jsonrpc":"2.0","id":"test","result":"0.03595702"}
    @Deprecated(
        "Use getFixRateForAmount for limits. A full-fledged replacement of these methods is also coming soon",
        ReplaceWith("getFixRateForAmount")
    )
    @POST("getMinAmount")
    fun getMinAmount(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Call<ChangellyResponse<Double>>

    @POST("getExchangeAmount")
    fun getExchangeAmount(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amountFrom") amount: Double,
    ): Call<ChangellyResponse<ChangellyGetExchangeAmountResponse>>

    //{
    // "jsonrpc":"2.0",
    // "id":"test",
    // "result":{"id":"39526c0eb6ba","apiExtraFee":"0","changellyFee":"0.5","payinExtraId":null,"status":"new","currencyFrom":"eth","currencyTo":"BTC","amountTo":0,"payinAddress":"0xdd0a917944efc6a371829053ad318a6a20ee1090","payoutAddress":"1J3cP281yiy39x3gcPaErDR6CSbLZZKzGz","createdAt":"2017-11-22T18:47:19.000Z"}
    // }
    @POST("createTransaction")
    fun createTransaction(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amountFrom") amount: Double,
        @Query("address") address: String,
    ): Call<ChangellyResponse<ChangellyTransactionOffer>>

    @POST("getCurrenciesFull")
    suspend fun getCurrenciesFull(): Response<ChangellyResponse<List<ChangellyCurrency>>>

    @POST("getFixRateForAmount")
    suspend fun getFixRateForAmount(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amountFrom") amount: BigDecimal = BigDecimal.ONE,
    ): Response<ChangellyListResponse<FixRate>>

    @Deprecated(
        "To get the fixed rate, you need to use getFixRateForAmount, but the transaction amount must be within limits",
        ReplaceWith("getFixRateForAmount")
    )
    @POST("getFixRate")
    suspend fun getFixRate(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ChangellyListResponse<FixRate>>

    @POST("createFixTransaction")
    suspend fun createFixTransaction(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amountFrom") amount: String,
        @Query("address") address: String,
        @Query("rateId") rateId: String,
        @Query("refundAddress") refundAddress: String,
    ): ChangellyResponse<ChangellyTransactionOffer>

    @POST("getTransactions")
    suspend fun getTransaction(
        @Query("id") id: String,
        @Query("limit") limit: Int = 1,
    ): Response<ChangellyResponse<List<ChangellyTransaction>>>

    @POST("getTransactions")
    suspend fun getTransactions(
        @Query("id") id: List<String>,
    ): Response<ChangellyResponse<List<ChangellyTransaction>>>

    companion object {
        const val BCH = "BCH"
        const val BTC = "BTC"
        const val FROM = "FROM"
        const val TO = "TO"
        const val AMOUNT = "AMOUNT"
        const val DESTADDRESS = "DESTADDRESS"
    }
}