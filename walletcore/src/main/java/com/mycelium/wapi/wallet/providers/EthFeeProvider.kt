package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.Eip1559Snapshot
import com.mycelium.wapi.wallet.eth.EthBlockchainService
import com.mycelium.wapi.wallet.eth.EthFeeLevel
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class EthFeeProvider(
    testnet: Boolean,
    private val service: EthBlockchainService,
    private val feeBacking: FeeEstimationsBacking
) : FeeProvider {

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
                feeBacking.updateFeeEstimation(newEstimation.feeEstimation())
                return@withContext newEstimation
            } catch (e: Exception) {
                return@withContext estimation
            }
        }
    }


    private fun getGasPriceEstimates(): FeeEstimationsGeneric {
        // EIP-1559 path: pull base fee + per-tier cap/tip from the dedicated
        // estimator and cache the snapshot so signing can recover the tip for
        // the chosen cap. The displayed tier value is the cap (maxFeePerGas).
        // Any failure (or a chain without 1559 support) falls back to the
        // legacy per-block gas price, and clears the snapshot so we don't sign
        // type-2 with caps that no longer match what the user sees.
        if (coinType.supportsEip1559) {
            try {
                val snapshot = service.eip1559FeeEstimation()
                service.cacheEip1559Fees(snapshot)
                return buildFromSnapshot(snapshot)
            } catch (e: Exception) {
                service.clearEip1559Fees()
            }
        } else {
            service.clearEip1559Fees()
        }
        return legacyEstimates()
    }

    private fun buildFromSnapshot(snapshot: Eip1559Snapshot): FeeEstimationsGeneric {
        fun cap(level: EthFeeLevel, fallback: Long): Value =
            snapshot.tier(level)?.maxFeePerGas?.let { Value.valueOf(coinType, it) }
                ?: Value.valueOf(coinType, fallback)
        return FeeEstimationsGeneric(
            low = cap(EthFeeLevel.LOW, 1000000000),
            economy = cap(EthFeeLevel.ECONOMY, 33000000000),
            normal = cap(EthFeeLevel.NORMAL, 67000000000),
            high = cap(EthFeeLevel.HIGH, 100000000000),
            lastCheck = System.currentTimeMillis()
        )
    }

    private fun legacyEstimates(): FeeEstimationsGeneric {
        val low = service.feeEstimation(PriceLevelSpeed.SAFE_LOW.block)
        val economy = service.feeEstimation(PriceLevelSpeed.AVERAGE.block)
        val fast = service.feeEstimation(PriceLevelSpeed.FAST.block)
        val fastest = service.feeEstimation(PriceLevelSpeed.FASTEST.block)
        return FeeEstimationsGeneric(
            low = Value.parse(coinType, low.result),
            economy = Value.parse(coinType, economy.result),
            normal = Value.parse(coinType, fast.result),
            high = Value.parse(coinType, fastest.result),
            lastCheck = System.currentTimeMillis()
        )
    }

    private enum class PriceLevelSpeed(val block: Int) {
        SAFE_LOW(15),
        AVERAGE(6),
        FAST(3),
        FASTEST(1),
    }
}