package com.mycelium.wapi.wallet.providers

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.net.URL


class EthFeeProvider(testnet: Boolean, private val feeBacking: FeeEstimationsBacking) :
    FeeProvider {

    override val coinType = if (testnet) {
        EthTest
    } else {
        EthMain
    }
    override var estimation: FeeEstimationsGeneric = feeBacking.getEstimationForCurrency(coinType)
        ?: FeeEstimationsGeneric(
            Value.valueOf(coinType, 1000000000),
            Value.valueOf(coinType, 33000000000),
            Value.valueOf(coinType, 67000000000),
            Value.valueOf(coinType, 100000000000),
            0
        )

    override suspend fun updateFeeEstimationsAsync() {
        estimation = withContext(Dispatchers.IO) {
            try {
                val newEstimation = getGasPriceEstimates()
                feeBacking.updateFeeEstimation(newEstimation)
                return@withContext newEstimation
            } catch (e: Exception) {
                return@withContext estimation
            }
        }
    }

    private fun getGasPriceEstimates(): FeeEstimationsGeneric {
        val mapper =
            ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(
            URL(GAS_PRICE_ESTIMATES_ADDRESS).readText(),
            GasPriceEstimates::class.java
        )
            .run {
                val low =
                    baseFeePerGas + priceLevels.first { it.speed == "safe_low" }.maxPriorityFeePerGas
                val economy =
                    baseFeePerGas + priceLevels.first { it.speed == "average" }.maxPriorityFeePerGas
                val normal =
                    baseFeePerGas + priceLevels.first { it.speed == "fast" }.maxPriorityFeePerGas
                val high =
                    baseFeePerGas + priceLevels.first { it.speed == "fastest" }.maxPriorityFeePerGas

                fun convert(value: BigInteger) = Value.valueOf(coinType, value)
                FeeEstimationsGeneric(
                    convert(low), convert(economy), convert(normal), convert(high),
                    System.currentTimeMillis()
                )
            }
    }

    /**
     * Estimates are provided by https://github.com/AlhimicMan/eip1559_gas_estimator
     */
    private class GasPriceEstimates {
        @JsonProperty("base_fee_per_gas")
        var baseFeePerGas: BigInteger = BigInteger.ZERO

        @JsonProperty("price_levels")
        var priceLevels: List<PriceLevels> = emptyList()

        class PriceLevels {
            var speed: String = ""

            @JsonProperty("max_fee_per_gas")
            var maxFeePerGas: BigInteger = BigInteger.ZERO

            @JsonProperty("max_priority_fee_per_gas")
            var maxPriorityFeePerGas: BigInteger = BigInteger.ZERO
        }
    }

    companion object {
        private const val GAS_PRICE_ESTIMATES_ADDRESS = "https://bb-eth.mycelium.com:8181/eth"
    }
}