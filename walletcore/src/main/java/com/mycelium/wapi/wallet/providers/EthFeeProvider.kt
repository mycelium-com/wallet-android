package com.mycelium.wapi.wallet.providers

import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.Eip1559Snapshot
import com.mycelium.wapi.wallet.eth.Eip1559Tier
import com.mycelium.wapi.wallet.eth.EthBlockchainService
import com.mycelium.wapi.wallet.eth.EthFeeLevel
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger


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
        if (!coinType.supportsEip1559) {
            service.clearEip1559Fees()
            return legacyEstimates()
        }
        // EIP-1559 path: pull base fee + per-tier cap/tip from the dedicated
        // estimator and cache the snapshot so signing can recover the tip for
        // the chosen cap. The displayed tier value is the cap (maxFeePerGas).
        try {
            val snapshot = service.eip1559FeeEstimation()
            service.cacheEip1559Fees(snapshot)
            return buildFromSnapshot(snapshot)
        } catch (e: Exception) {
            // No estimator configured or it's unreachable. The previously cached
            // snapshot stays valid until we have something better: clearing it
            // here would make the next send sign legacy at a cap the user chose
            // as a 1559 cap. Blockbook can still give us the base fee.
            logger.log(Level.WARNING, "EIP-1559 estimator failed, deriving tiers from blockbook base fee", e)
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
        val readings = PriceLevelSpeed.values().map {
            Value.parse(coinType, service.feeEstimation(it.block).result)
        }
        return legacyFallbackEstimates(coinType, readings, service, System.currentTimeMillis())
    }

    private companion object {
        val logger: Logger = Logger.getLogger(EthFeeProvider::class.java.simpleName)
    }

    /** Ordered low → high; [legacyFallbackEstimates] relies on that order. */
    private enum class PriceLevelSpeed(val block: Int) {
        SAFE_LOW(15),
        AVERAGE(6),
        FAST(3),
        FASTEST(1),
    }
}

/**
 * Fee tiers from blockbook's per-block `estimatefee` answers (ordered low → high), for when
 * the dedicated EIP-1559 estimator is unavailable.
 *
 * On a chain without EIP-1559 the four answers are the tiers, signed legacy.
 *
 * On an EIP-1559 chain blockbook's Ethereum `estimatefee` ignores the block target and
 * answers `baseFeePerGas + 1000 wei` for every one of them. The four values still differ
 * now and then — a block lands between the calls, or `endpoints.random()` hits servers at
 * different heights — and that used to be read as "the server differentiates", which
 * dropped the cached snapshot and signed the next send legacy at roughly the base fee
 * (Sepolia 0x7a4237e0…: picker showed 2.2–2.7 Gwei caps, tx went out type-0 at 1.05 Gwei
 * and only got mined because the base fee fell). Every answer is a base-fee reading, so the
 * highest one is taken as the base fee and the tiers are derived from it, exactly as for the
 * degenerate all-equal answer. A 1559 chain never signs legacy from this path.
 */
internal fun legacyFallbackEstimates(
    coinType: EthCoin,
    readings: List<Value>,
    service: EthBlockchainService,
    now: Long,
): FeeEstimationsGeneric {
    require(readings.size == 4) { "expected low/economy/normal/high, got ${readings.size}" }
    if (!coinType.supportsEip1559) {
        service.clearEip1559Fees()
        return FeeEstimationsGeneric(readings[0], readings[1], readings[2], readings[3], now)
    }
    val base = readings.maxOf { it.value }
    val tiers = deriveFeeTiers(base)
    service.cacheEip1559Fees(Eip1559Snapshot(base, tiers))
    return FeeEstimationsGeneric(
        low = Value.valueOf(coinType, tiers[0].maxFeePerGas),
        economy = Value.valueOf(coinType, tiers[1].maxFeePerGas),
        normal = Value.valueOf(coinType, tiers[2].maxFeePerGas),
        high = Value.valueOf(coinType, tiers[3].maxFeePerGas),
        lastCheck = now
    )
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
