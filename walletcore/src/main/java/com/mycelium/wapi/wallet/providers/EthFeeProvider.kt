package com.mycelium.wapi.wallet.providers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.utils.Convert
import java.net.URL


class EthFeeProvider(testnet: Boolean, private val feeBacking: FeeEstimationsBacking) : FeeProvider {
    override val coinType = if (testnet) {
        EthTest
    } else {
        EthMain
    }
    override var estimation: FeeEstimationsGeneric = feeBacking.getEstimationForCurrency(coinType)
            ?: FeeEstimationsGeneric(Value.valueOf(coinType, 1000000000),
                    Value.valueOf(coinType, 33000000000),
                    Value.valueOf(coinType, 67000000000),
                    Value.valueOf(coinType, 100000000000),
                    0)

    override suspend fun updateFeeEstimationsAsync() {
        estimation = withContext(Dispatchers.IO) {
            try {
                val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                val newEstimation = mapper.readValue(URL("https://ethgasstation.info/json/ethgasAPI.json").readText(),
                        GasStationEstimation::class.java)
                        .run {
                            fun convert(value: Double) = Value.valueOf(coinType,
                                    Convert.toWei(value.toBigDecimal(), Convert.Unit.GWEI).toBigInteger())
                            FeeEstimationsGeneric(convert(safeLow), convert(average), convert(fast), convert(fastest),
                                    System.currentTimeMillis()
                            )
                        }
                feeBacking.updateFeeEstimation(newEstimation)
                return@withContext newEstimation
            } catch (e: Exception) {
                return@withContext estimation
            }
        }
    }

    /**
     * Gas station estimates are provided in GWEI*10
     */
    private class GasStationEstimation {
        var fastest: Double = 0.0
            get() = field / 10
        var fast: Double = 0.0
            get() = field / 10
        var average: Double = 0.0
            get() = field / 10
        var safeLow: Double = 0.0
            get() = field / 10
    }
}