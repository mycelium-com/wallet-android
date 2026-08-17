package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.tron.remote.AccountResourceInfo
import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger

/** Fee estimation result for a TRON transaction. All sun amounts are in TRX's smallest unit. */
data class TronFeeEstimation(
    /** Bandwidth (bytes) the tx will consume. */
    val bandwidth: Long,
    /** Energy the tx will consume (smart contract calls only). */
    val energy: Long,
    /** Fee expected to be actually charged (sun). */
    val feeInSun: Long,
    val feeValue: Value,
    /** Free/staked bandwidth covers the tx. */
    val useFreeBandwidth: Boolean,
    /** Staked energy covers the tx. */
    val useStakedEnergy: Boolean,
    /** Recipient not yet activated on-chain — a TRX transfer burns the account creation fee. */
    val recipientNeedsActivation: Boolean,
    /** Cap to put into `raw.fee_limit` (contract calls) / to reserve for the tx. */
    val recommendedFeeLimit: BigInteger,
) {
    fun toTransactionData() = TronTransactionData(
        bandwidth = BigInteger.valueOf(bandwidth),
        energy = BigInteger.valueOf(energy),
        feeLimit = recommendedFeeLimit,
        estimatedFee = BigInteger.valueOf(feeInSun),
    )
}

/**
 * Estimates TRON fees.
 *
 * TRON fee model:
 * - Bandwidth is needed by every tx (tx size in bytes). Each account gets ~600 free
 *   bandwidth per day; beyond that (or from staking) 1 bandwidth = 1000 sun.
 * - Energy is needed by smart-contract calls only (TRC20 transfers). It comes from staking or
 *   is bought at the chain's energy price (`getEnergyFee`, ~420 sun) when the tx executes.
 * - fee_limit caps the TRX a contract call may burn; if execution needs more, the whole limit
 *   is burned and the call fails (OUT_OF_ENERGY), so the cap must be generous.
 * - A TRX transfer to a never-used address additionally burns the 1 TRX activation fee (+0.1 TRX).
 *
 * All calls are blocking network calls (cached where sensible) — run them off the main thread.
 */
class TronFeeEstimator(private val blockchainService: TronBlockchainService) {
    private val logger = Logger.getLogger(TronFeeEstimator::class.simpleName)

    private data class CachedResources(val resources: AccountResourceInfo, val timestamp: Long)

    private val resourcesCache = HashMap<String, CachedResources>()
    @Volatile
    private var cachedEnergyPrice: Long? = null
    @Volatile
    private var energyPriceCacheTime = 0L

    fun estimateTrxTransferFee(ownerAddress: String, coinType: AssetInfo, toAddress: String? = null,
                               transactionSize: Long = TRX_TRANSFER_BANDWIDTH): TronFeeEstimation {
        return try {
            val resources = getCachedAccountResources(ownerAddress)
            val useFreeBandwidth = resources.bandwidthAvailable >= transactionSize
            val bandwidthFee = if (useFreeBandwidth) 0L else transactionSize * SUN_PER_BANDWIDTH
            val needsActivation = toAddress != null && !blockchainService.isAccountActive(toAddress)
            val activationFee = if (needsActivation) ACCOUNT_ACTIVATION_FEE_SUN else 0L
            val fee = bandwidthFee + activationFee
            TronFeeEstimation(transactionSize, 0, fee, Value.valueOf(coinType, fee), useFreeBandwidth, false,
                needsActivation, BigInteger.valueOf(fee))
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error estimating TRX transfer fee, using default: ${e.message}")
            defaultTrxEstimation(transactionSize, coinType)
        }
    }

    /**
     * @param amount token amount to simulate; when null (sender holds no tokens yet, so any
     *        simulation would revert) the typical transfer energy is assumed instead
     */
    fun estimateTrc20TransferFee(ownerAddress: String, contractAddress: String, toAddress: String,
                                 amount: BigInteger?, baseCoinType: AssetInfo): TronFeeEstimation {
        return try {
            val resources = getCachedAccountResources(ownerAddress)
            val energyPrice = getCachedEnergyPrice()
            val estimatedEnergy = if (amount != null && amount.signum() > 0) {
                blockchainService.estimateTrc20TransferEnergy(ownerAddress, contractAddress, toAddress, amount)
            } else {
                TYPICAL_TRC20_ENERGY
            }
            calculateTrc20Fee(resources, estimatedEnergy, energyPrice, baseCoinType)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Error estimating TRC20 transfer fee, using default: ${e.message}")
            defaultTrc20Estimation(baseCoinType)
        }
    }

    private fun calculateTrc20Fee(resources: AccountResourceInfo, estimatedEnergy: Long, energyPrice: Long,
                                  baseCoinType: AssetInfo): TronFeeEstimation {
        val requiredBandwidth = TRC20_TRANSFER_BANDWIDTH
        val useFreeBandwidth = resources.bandwidthAvailable >= requiredBandwidth
        val bandwidthCost = if (useFreeBandwidth) 0L else requiredBandwidth * SUN_PER_BANDWIDTH
        val useStakedEnergy = resources.energyAvailable >= estimatedEnergy
        val energyToPayFor = if (useStakedEnergy) 0L else maxOf(0L, estimatedEnergy - resources.energyAvailable)
        val energyCost = energyToPayFor * energyPrice
        val totalFee = bandwidthCost + energyCost
        // fee_limit must cover the worst case: broadcasting with a limit below the execution
        // cost burns the whole limit without transferring. Cover the full energy at the
        // current price with a margin even when staked energy is expected to pay for it.
        val worstCase = (requiredBandwidth * SUN_PER_BANDWIDTH + estimatedEnergy * energyPrice) * FEE_LIMIT_MULTIPLIER
        val feeLimit = maxOf(worstCase.toLong(), MIN_TRC20_FEE_LIMIT)
        return TronFeeEstimation(requiredBandwidth, estimatedEnergy, totalFee, Value.valueOf(baseCoinType, totalFee),
            useFreeBandwidth, useStakedEnergy, false, BigInteger.valueOf(feeLimit))
    }

    private fun defaultTrxEstimation(transactionSize: Long, coinType: AssetInfo): TronFeeEstimation {
        val fee = transactionSize * SUN_PER_BANDWIDTH
        return TronFeeEstimation(transactionSize, 0, fee, Value.valueOf(coinType, fee), false, false, false,
            BigInteger.valueOf(fee))
    }

    private fun defaultTrc20Estimation(baseCoinType: AssetInfo): TronFeeEstimation {
        val energyPrice = cachedEnergyPrice ?: TronBlockchainService.DEFAULT_ENERGY_PRICE
        val total = TRC20_TRANSFER_BANDWIDTH * SUN_PER_BANDWIDTH + TronBlockchainService.DEFAULT_TRC20_ENERGY * energyPrice
        val feeLimit = maxOf((total * FEE_LIMIT_MULTIPLIER).toLong(), MIN_TRC20_FEE_LIMIT)
        return TronFeeEstimation(TRC20_TRANSFER_BANDWIDTH, TronBlockchainService.DEFAULT_TRC20_ENERGY, total,
            Value.valueOf(baseCoinType, total), false, false, false, BigInteger.valueOf(feeLimit))
    }

    private fun getCachedEnergyPrice(): Long {
        val now = System.currentTimeMillis()
        cachedEnergyPrice?.let { if (now - energyPriceCacheTime < ENERGY_PRICE_CACHE_TTL_MS) return it }
        val price = blockchainService.getEnergyPrice()
        cachedEnergyPrice = price
        energyPriceCacheTime = now
        return price
    }

    @Synchronized
    private fun getCachedAccountResources(address: String): AccountResourceInfo {
        val now = System.currentTimeMillis()
        resourcesCache[address]?.let { if (now - it.timestamp < RESOURCES_CACHE_TTL_MS) return it.resources }
        val resources = blockchainService.getAccountResources(address)
        resourcesCache[address] = CachedResources(resources, now)
        return resources
    }

    /** Call after a tx was sent so the next estimate sees the consumed resources. */
    @Synchronized
    fun invalidateCache(address: String) {
        resourcesCache.remove(address)
    }

    fun getAccountResources(address: String): AccountResourceInfo = getCachedAccountResources(address)

    companion object {
        const val SUN_PER_BANDWIDTH = 1000L
        const val TRX_TRANSFER_BANDWIDTH = 268L
        const val TRC20_TRANSFER_BANDWIDTH = 345L
        /** Typical energy of a TRC20 transfer to a recipient that already holds the token. */
        const val TYPICAL_TRC20_ENERGY = 65_000L
        /** 1 TRX account-creation fee + 100 bandwidth not coverable by the free quota (0.1 TRX). */
        const val ACCOUNT_ACTIVATION_FEE_SUN = 1_100_000L
        const val FEE_LIMIT_MULTIPLIER = 1.2
        /** TRC20 contracts can require significant energy, especially for first-time transfers. */
        const val MIN_TRC20_FEE_LIMIT = 15_000_000L
        private const val RESOURCES_CACHE_TTL_MS = 10_000L
        private const val ENERGY_PRICE_CACHE_TTL_MS = 300_000L
    }
}
