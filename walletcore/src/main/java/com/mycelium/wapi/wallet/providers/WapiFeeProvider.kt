package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class WapiFeeProvider(private val wapi: Wapi, private val feeBacking: FeeEstimationsBacking) : FeeProvider {
    abstract override val coinType: AssetInfo

    abstract override var estimation: FeeEstimationsGeneric

    override suspend fun updateFeeEstimationsAsync() {
        // we try to get fee estimation from server
        estimation = withContext(Dispatchers.IO) {
            try {
                val response = wapi.minerFeeEstimations
                val oldStyleFeeEstimation = response.result.feeEstimation
                fun convert(blocks: Int): Value {
                    val estimate = oldStyleFeeEstimation.getEstimation(blocks)
                    return Value.valueOf(coinType, estimate.longValue)
                }

                val newEstimation = FeeEstimationsGeneric(convert(20), convert(10), convert(3), convert(1),
                        System.currentTimeMillis()
                )
                //if all ok we return requested new fee estimation
                feeBacking.updateFeeEstimation(newEstimation)
                return@withContext newEstimation
            } catch (ex: WapiException) {
                return@withContext estimation
            }
        }
    }
}