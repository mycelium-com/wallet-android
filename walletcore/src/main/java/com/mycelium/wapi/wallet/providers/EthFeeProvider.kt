package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.Eip1559Snapshot
import com.mycelium.wapi.wallet.eth.Eip1559Tier
import com.mycelium.wapi.wallet.eth.EthBlockchainService
import com.mycelium.wapi.wallet.eth.EthFeeLevel
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger


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
        if (coinType.supportsEip1559) {
            try {
                val snapshot = service.eip1559FeeEstimation()
                service.cacheEip1559Fees(snapshot)
                return buildFromSnapshot(snapshot)
            } catch (e: Exception) {
                // No estimator configured or it's unreachable. Fall through —
                // legacyEstimates() can usually still recover the base fee and
                // build an equivalent snapshot itself.
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
        val low = Value.parse(coinType, service.feeEstimation(PriceLevelSpeed.SAFE_LOW.block).result)
        val economy = Value.parse(coinType, service.feeEstimation(PriceLevelSpeed.AVERAGE.block).result)
        val normal = Value.parse(coinType, service.feeEstimation(PriceLevelSpeed.FAST.block).result)
        val high = Value.parse(coinType, service.feeEstimation(PriceLevelSpeed.FASTEST.block).result)

        // Blockbook's Ethereum estimatefee ignores the block target: it answers
        // `baseFeePerGas + 1000 wei` for 1, 3, 6 and 15 blocks alike. Passed
        // straight through, all four tiers in the fee picker showed the same
        // number — the choice did nothing — and that number left about a
        // thousand wei of priority fee, so a legacy tx sent at it only got
        // mined if the base fee happened to fall. Recognise the degenerate
        // answer, read it as the base fee, and derive tiers that differ and
        // that survive the base fee rising between now and inclusion.
        val degenerate = low == economy && economy == normal && normal == high
        if (!degenerate) {
            // The server really does differentiate; trust it and sign legacy,
            // since we have no base fee to build 1559 caps around.
            service.clearEip1559Fees()
            return FeeEstimationsGeneric(low, economy, normal, high, System.currentTimeMillis())
        }
        // The degenerate answer IS the base fee (plus a negligible 1000 wei), which
        // is everything a 1559 estimate needs: cap = baseFee * headroom + tip. So
        // rather than giving up on type-2 whenever the dedicated estimator is
        // missing, derive the snapshot here — that is what makes the caps the user
        // picks and the tip we sign with agree.
        val base = low.value
        val tiers = deriveFeeTiers(base)
        if (coinType.supportsEip1559) {
            service.cacheEip1559Fees(Eip1559Snapshot(base, tiers))
        } else {
            service.clearEip1559Fees()
        }
        return FeeEstimationsGeneric(
            low = Value.valueOf(coinType, tiers[0].maxFeePerGas),
            economy = Value.valueOf(coinType, tiers[1].maxFeePerGas),
            normal = Value.valueOf(coinType, tiers[2].maxFeePerGas),
            high = Value.valueOf(coinType, tiers[3].maxFeePerGas),
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

/**
 * Per-tier caps and tips built from the current base fee, for when no dedicated
 * EIP-1559 estimator is reachable. Each cap is `baseFee * headroom% + tip`: the
 * headroom absorbs the base fee climbing between signing and inclusion (it can
 * rise 12.5% per block), and the tip is what actually buys priority.
 */
internal fun deriveFeeTiers(baseFeeWei: BigInteger): List<Eip1559Tier> = listOf(
    Triple(EthFeeLevel.LOW, 105L, 50_000_000L),
    Triple(EthFeeLevel.ECONOMY, 115L, 200_000_000L),
    Triple(EthFeeLevel.NORMAL, 135L, 700_000_000L),
    Triple(EthFeeLevel.HIGH, 180L, 2_000_000_000L),
).map { (level, headroomPercent, tipWei) ->
    val cap = baseFeeWei * BigInteger.valueOf(headroomPercent) / BigInteger.valueOf(100) +
            BigInteger.valueOf(tipWei)
    Eip1559Tier(level, cap, BigInteger.valueOf(tipWei))
}
