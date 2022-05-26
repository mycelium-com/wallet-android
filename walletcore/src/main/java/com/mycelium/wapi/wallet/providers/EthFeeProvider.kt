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
                FeeEstimationsGeneric(
                    convertBigIntegerToValue(getPrice(PriceLevelSpeed.SAFE_LOW)),
                    convertBigIntegerToValue(getPrice(PriceLevelSpeed.AVERAGE)),
                    convertBigIntegerToValue(getPrice(PriceLevelSpeed.FAST)),
                    convertBigIntegerToValue(getPrice(PriceLevelSpeed.FASTEST)),
                    System.currentTimeMillis()
                )
            }
    }

    private fun convertBigIntegerToValue(value: BigInteger) = Value.valueOf(coinType, value)

    /**
     * Estimates are provided by https://github.com/AlhimicMan/eip1559_gas_estimator
     */
    private class GasPriceEstimates {
        @JsonProperty("base_fee_per_gas")
        var baseFeePerGas: BigInteger = BigInteger.ZERO

        @JsonProperty("price_levels")
        var priceLevels: List<PriceLevels> = emptyList()

        private class PriceLevels {
            var speed: PriceLevelSpeed = PriceLevelSpeed.UNKNOWN

            @JsonProperty("max_fee_per_gas")
            var maxFeePerGas: BigInteger = BigInteger.ZERO

            @JsonProperty("max_priority_fee_per_gas")
            var maxPriorityFeePerGas: BigInteger = BigInteger.ZERO
        }

        fun getPrice(priceLevelSpeed: PriceLevelSpeed) =
            baseFeePerGas + priceLevels.first { it.speed == priceLevelSpeed }.maxPriorityFeePerGas
    }

    private enum class PriceLevelSpeed {
        @JsonProperty("safe_low")
        SAFE_LOW,
        @JsonProperty("average")
        AVERAGE,
        @JsonProperty("fast")
        FAST,
        @JsonProperty("fastest")
        FASTEST,
        UNKNOWN
    }

    companion object {
        private const val GAS_PRICE_ESTIMATES_ADDRESS = "https://bb-eth.mycelium.com:8181/eth"
    }
}