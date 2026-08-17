package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigInteger

/**
 * Unsigned/signed TRON transaction. For TRX transfers [type] is the TRX coin and [tronValue]
 * the amount; for TRC20 transfers [type] is still TRX (fees are paid in TRX), [tronValue] is
 * zero, [tokenValue] carries the token amount and [toAddress] the recipient (the on-chain `to`
 * of the contract call is the token contract, set at signing time).
 */
class TronTransaction(
    type: CryptoCurrency,
    val toAddress: String,
    val tronValue: Value,
    /** Max TRX (sun) that may be burned for the tx: bandwidth (+ energy for contract calls). */
    val feeLimit: BigInteger,
    val bandwidth: BigInteger,
    val energy: BigInteger,
    /** Estimated fee actually charged (sun) — what the UI displays; feeLimit is the cap. */
    val estimatedFee: BigInteger = feeLimit,
    val estimatedBandwidthUsed: Int = TRX_TRANSFER_BANDWIDTH,
    val tokenValue: Value? = null,
    /** Validity of the signed tx (ms); null = [TronTransactionBuilder.DEFAULT_EXPIRATION_MS]. Longer values bound how long a third party holding the signed tx (energy service) can still broadcast it. */
    val expirationMs: Long? = null,
) : Transaction(type) {
    /** `Transaction.raw` protobuf bytes. */
    var rawData: ByteArray? = null
    /** Full signed `Transaction` protobuf (raw_data + signature) as broadcast. */
    var signedBytes: ByteArray? = null
    var txHash: ByteArray? = null
    /** 65-byte secp256k1 signature. */
    var signature: ByteArray? = null
    /** TrustWallet-Core-compatible `SigningOutput` protobuf (the energy service wire format). */
    var signingOutput: ByteArray? = null
    /** `raw_data.expiration` actually signed (ms). */
    var signedExpirationMs: Long? = null

    override fun getId() = txHash

    override fun txBytes() = signedBytes

    override fun totalFee(): Value = Value.valueOf(type, estimatedFee)

    override fun getEstimatedTransactionSize() = estimatedBandwidthUsed

    companion object {
        const val TRX_TRANSFER_BANDWIDTH = 268
    }
}
