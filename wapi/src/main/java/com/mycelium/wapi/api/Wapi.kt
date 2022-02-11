/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycelium.wapi.api

import com.mycelium.wapi.api.Wapi.ElectrumxError
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest
import com.mycelium.wapi.api.WapiResponse
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest
import com.mycelium.wapi.api.response.QueryTransactionInventoryResponse
import com.mycelium.wapi.api.request.GetTransactionsRequest
import com.mycelium.wapi.api.response.GetTransactionsResponse
import com.mycelium.wapi.api.request.BroadcastTransactionRequest
import com.mycelium.wapi.api.response.BroadcastTransactionResponse
import com.mycelium.wapi.api.request.CheckTransactionsRequest
import com.mycelium.wapi.api.response.CheckTransactionsResponse
import com.mycelium.wapi.api.request.GetExchangeRatesRequest
import com.mycelium.wapi.api.response.GetExchangeRatesResponse
import com.mycelium.wapi.api.response.PingResponse
import com.mycelium.wapi.api.request.ErrorCollectorRequest
import com.mycelium.wapi.api.response.ErrorCollectorResponse
import com.mycelium.wapi.api.request.VersionInfoExRequest
import com.mycelium.wapi.api.response.VersionInfoExResponse
import com.mycelium.wapi.api.response.MinerFeeEstimationResponse
import java.lang.IllegalArgumentException

interface Wapi {
    /**
     * This codes are used to identify Bitcoind errors which are passed through electrumx.
     * https://github.com/bitcoin/bitcoin/blob/bcffd8743e7f9cf286e641a0df8df25241a9238c/src/consensus/validation.h#L16
     */
    enum class ElectrumxError(private val externalErrorCode: Int, val errorCode: Int) {
        REJECT_MALFORMED(0x01, 101), REJECT_DUPLICATE(0x12, 102), REJECT_NONSTANDARD(0x40, 103), REJECT_INSUFFICIENT_FEE(0x42, 104);

        companion object {
            fun getErrorByCode(errorCode: Int): ElectrumxError {
                for (error in values()) {
                    if (error.externalErrorCode == errorCode) {
                        return error
                    }
                }
                throw IllegalArgumentException("")
            }
        }
    }

    /**
     * Query the full set of unspent outputs for a set of addresses
     * Example HTTP POST:
     * curl  -k -X POST -H "Content-Type: application/json"
     * -d '{"version":1,"addresses":["msxh4zZoVwdRXfgmAYYo2MpNrJi4snrH6C","mfv9QuzUD7ZtnHxfpVX2859hs2ZHC8TG16","mpii6kiLM5HffaJdeD4Smnpv5eWo7qfKQ5"]}'
     * https://144.76.165.115/wapitestnet/wapi/queryUnspentOutputs
     */
    @Deprecated("this service has reached end of life and will be replaced by electrumx")
    suspend fun queryUnspentOutputs(request: QueryUnspentOutputsRequest): WapiResponse<QueryUnspentOutputsResponse>

    /**
     * Query the transaction inventory of a set of addresses with a limit on how many transaction IDs to retrieve
     * Example HTTP POST:
     * curl   -k -X POST -H "Content-Type: application/json"
     * -d '{"version":1,"addresses":["mfd7QG4vn2U4U5BgnTuw7dmjKsutDxkK6b","mysJrGMsYht9u3gBvKHFcNJsVEmaEPhUGA","mvMyQXzaHk7Z6u3vsbzT7qmQJo225ma9g3"],"limit":1000}'
     * https://144.76.165.115/wapitestnet/wapi/queryTransactionInventory
     */
    @Deprecated("this service has reached end of life and will be replaced by electrumx")
    suspend fun queryTransactionInventory(request: QueryTransactionInventoryRequest): WapiResponse<QueryTransactionInventoryResponse>

    /**
     * Get a set of transactions from a set of transaction IDs
     * Example HTTP POST:
     * curl  -k -X POST -H "Content-Type: application/json"
     * -d '{"version":1,"txIds":["1513b9b160ef6b20bbb06b7bb6e7364e58e27e1df53f8f7e12e67f17d46ad198"]}'
     * https://144.76.165.115/wapitestnet/wapi/getTransactions
     */
    @Deprecated("this service has reached end of life and will be replaced by electrumx")
    suspend fun getTransactions(request: GetTransactionsRequest): WapiResponse<GetTransactionsResponse>

    /**
     * Broadcast a transaction
     * Example HTTP POST:
     * curl  -k -X POST -H "Content-Type: application/json"
     * -d '{"version":1,"rawTransaction":"AQAAAAHqHGsQSIun5hjDDWm7iFMwm85xNLt+HBfI3LS3uQHnSQEAAABrSDBFAiEA6rlGk4wgIL3TvC2YHK4XiBW2vPYg82iCgnQi+YOUwqACIBpzVk756/07SRORT50iRZvEGUIn3Lh3bhaRE1aUMgZZASECDFl9wEYDCvB1cJY6MbsakfKQ9tbQhn0eH9C//RI2iE//////ApHwGgAAAAAAGXapFIzWtPXZR7lk8RtvE0FDMHaLtsLCiKyghgEAAAAAABl2qRSuzci59wapXUEzwDzqKV9nIaqwz4isAAAAAA=="}'
     * https://144.76.165.115/wapitestnet/wapi/broadcastTransaction
     */
    @Deprecated("this service has reached end of life and will be replaced by electrumx")
    fun broadcastTransaction(request: BroadcastTransactionRequest): WapiResponse<BroadcastTransactionResponse>

    /**
     * Check the status of a transaction.
     *
     *
     * This allows you to check whether it exists, has confirmed, or got its
     * timestamp updated.
     * Example HTTP POST:
     * curl   -k -X POST -H "Content-Type: application/json"
     * -d '{"txIds":["1513b9b160ef6b20bbb06b7bb6e7364e58e27e1df53f8f7e12e67f17d46ad198"]}'
     * https://144.76.165.115/wapitestnet/wapi/checkTransactions
     */
    @Deprecated("this service has reached end of life and will be replaced by electrumx")
    suspend fun checkTransactions(request: CheckTransactionsRequest): WapiResponse<CheckTransactionsResponse>

    /**
     * Get exchange rates
     *
     *
     * Get the exchange rates for available exchanges converted to a specific fiat currency
     * Example HTTP POST:
     * curl  -k -X POST -H "Content-Type: application/json"
     * -d '{"version":1,"currency":"USD"}'
     * https://144.76.165.115/wapitestnet/wapi/queryExchangeRates
     */
    fun getExchangeRates(request: GetExchangeRatesRequest?): WapiResponse<GetExchangeRatesResponse?>?

    /**
     * Check if the wapi-service is running
     *
     */
    fun ping(): WapiResponse<PingResponse?>?

    /**
     * Report a app crash back to the server which sends a mail to the devolopers
     *
     */
    fun collectError(request: ErrorCollectorRequest?): WapiResponse<ErrorCollectorResponse?>?

    /**
     * Get the current version-number for a certain branch (Android, iOS, ..)
     * and also get a collection of eventually blocked features if there is a bug discovered
     *
     * returns null (empty object) if there are no warnings or important updates available for this branch/version
     *
     * curl -k -X POST -H "Content-Type: application/json" -d '{"branch":"android", "currentVersion":"2.3.1", "locale":"de" }' https://144.76.165.115/wapitestnet/wapi/getVersionEx
     *
     */
    fun getVersionInfoEx(request: VersionInfoExRequest?): WapiResponse<VersionInfoExResponse?>?

    /**
     * Get the current miner fee estimation in Bitcoin-per-kB, to be included within the next 1,2 or 4 Blocks
     *
     *
     * returns an object with {1: fee_1, 2: fee_2, 4: fee_4}  (where fee_n is the fee needed per kB to be
     * included in the next n-Blocks, in satoshis
     *
     * curl -k -X POST -H "Content-Type: application/json" -d '{}' https://144.76.165.115/wapitestnet/wapi/getMinerFeeEstimations
     */
    suspend fun getMinerFeeEstimations(): WapiResponse<MinerFeeEstimationResponse>

    companion object {
        /**
         * The current version of the API
         */
        const val VERSION = 1
        const val ERROR_CODE_SUCCESS = 0
        const val ERROR_CODE_NO_SERVER_CONNECTION = 1
        const val ERROR_CODE_INCOMPATIBLE_API_VERSION = 2
        const val ERROR_CODE_INTERNAL_CLIENT_ERROR = 3
        const val ERROR_CODE_INVALID_SESSION = 4
        const val ERROR_CODE_INVALID_ARGUMENT = 5
        const val ERROR_CODE_PARSING_ERROR = 6000
        const val ERROR_CODE_INTERNAL_SERVER_ERROR = 99
        const val ERROR_CODE_RESPONSE_TOO_LARGE = -32600
        const val MYCELIUM_VERSION_HEADER = "MyceliumVersion"
    }
}