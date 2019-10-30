package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class BtcFeeProvider(testnet: Boolean, private val wapi: Wapi, private val feeBacking: FeeEstimationsBacking) : FeeProvider {
    final override val coinType = if (testnet) {
        BitcoinTest.get()!!
    } else {
        BitcoinMain.get()!!
    }

    override var estimation = feeBacking.getEstimationForCurrency(coinType)
            ?: FeeEstimationsGeneric(Value.valueOf(coinType, 1000),
                    Value.valueOf(coinType, 3000),
                    Value.valueOf(coinType, 6000),
                    Value.valueOf(coinType, 8000),
                    0)

    override suspend fun updateFeeEstimationsAsync() {
        // we try to get fee estimation from server
        estimation = withContext(Dispatchers.IO) {
            try {
                val response = wapi.getMinerFeeEstimations()
                val oldStyleFeeEstimation = response.getResult().feeEstimation
                val lowPriority = oldStyleFeeEstimation.getEstimation(20)
                val normal = oldStyleFeeEstimation.getEstimation(3)
                val economy = oldStyleFeeEstimation.getEstimation(10)
                val high = oldStyleFeeEstimation.getEstimation(1)
                val newEstimation = FeeEstimationsGeneric(
                        Value.valueOf(coinType, lowPriority!!.longValue),
                        Value.valueOf(coinType, economy!!.longValue),
                        Value.valueOf(coinType, normal!!.longValue),
                        Value.valueOf(coinType, high!!.longValue),
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