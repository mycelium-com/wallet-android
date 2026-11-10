package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.TransactionData
import java.math.BigInteger

/**
 * Optional per-send parameters, typically filled from a [TronFeeEstimation]. When absent the
 * account falls back to the fee passed to `createTx` as feeLimit and typical resource costs.
 */
data class TronTransactionData(
    val bandwidth: BigInteger? = null,
    val energy: BigInteger? = null,
    /** Cap on TRX burned by the tx (sun). For TRC20 this is `raw.fee_limit`. */
    val feeLimit: BigInteger? = null,
    /** Expected fee actually charged (sun) for display / max-spendable; defaults to feeLimit. */
    val estimatedFee: BigInteger? = null,
    /** Whether to require the TRX (parent) account to cover feeLimit — TRC20 only. */
    val checkFeeBalance: Boolean = true,
    /** Validity of the signed tx (ms); null = default. */
    val expirationMs: Long? = null,
    /**
     * Fee charged in the token itself (energy service), in the token's smallest unit. It is not
     * part of the transfer, so it has to come off what the account can spend — see
     * [com.mycelium.wapi.wallet.trc20.TRC20Account.calculateMaxSpendableAmount].
     */
    val tokenFee: BigInteger? = null,
) : TransactionData
