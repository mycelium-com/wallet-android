package com.mrd.bitlib

import com.mrd.bitlib.model.CompactInt


class FeeEstimator(private val legacyInputs: Int, private val p2shSegwitInputs: Int, private val bechInputs: Int,
                   private val legacyOutputs: Int, private val p2shOutputs: Int, private val bechOutputs: Int,
                   private val minerFeePerKb: Long) {

    /**
     * Estimate the size of a transaction by taking the number of inputs and outputs into account. This allows us to
     * give a good estimate of the final transaction size, and determine whether out fee size is large enough.*/
    fun estimateTransactionSize(): Int {
        val totalOutputsSize = SEGWIT_NATIVE_OUTPUT_SIZE * bechOutputs + SEGWIT_COMPAT_OUTPUT_SIZE * p2shOutputs +
                OUTPUT_SIZE * legacyOutputs

        var estimateExceptInputs = 0
        if (bechInputs + p2shSegwitInputs > 0) {
            estimateExceptInputs += 2
        }
        estimateExceptInputs += 4 // Version info
        estimateExceptInputs += CompactInt.toBytes((legacyInputs + p2shSegwitInputs + bechInputs).toLong()).size // num input encoding. Usually 1. >253 inputs -> 3
        estimateExceptInputs += CompactInt.toBytes((legacyOutputs + p2shOutputs + bechOutputs).toLong()).size // num output encoding. Usually 1. >253 outputs -> 3
        estimateExceptInputs += totalOutputsSize
        estimateExceptInputs += 4 // nLockTime

        val estimateWithSignatures = estimateExceptInputs + MAX_INPUT_SIZE * (legacyInputs + p2shSegwitInputs) +
                MAX_BECH32_INPUT_SIZE * bechInputs
        val estimateWithoutWitness = estimateExceptInputs + MAX_SEGWIT_COMPAT_INPUT_SIZE * p2shSegwitInputs +
                MAX_SEGWIT_NATIVE_INPUT_SIZE * bechInputs + MAX_INPUT_SIZE * legacyInputs

        return (estimateWithoutWitness * 3 + estimateWithSignatures) / 4
    }

    /**
     * Returns the estimate needed fee in satoshis for a default P2PKH transaction with a certain number
     * of inputs and outputs and the specified per-kB-fee
     **/
    fun estimateFee(): Long {
        // fee is based on the size of the transaction, we have to pay for
        // every 1000 bytes
        val txSizeKb = (estimateTransactionSize() / 1000.0).toFloat() //in kilobytes
        return (txSizeKb * minerFeePerKb).toLong()
    }

    companion object {
        // hash size 32 + output index size 4 + script length 1 + max. script size for compressed keys 107 + sequence number 4
        // also see https://github.com/bitcoin/bitcoin/blob/master/src/primitives/transaction.h#L190
        private const val MAX_INPUT_SIZE = 32 + 4 + 1 + 107 + 4
        // BECH32 utxos does not contain scriptSig and scriptPubKey is 3 bytes smaller
        private const val MAX_BECH32_INPUT_SIZE = 32 + 4 + 1 + 107 + 4 - 23
        // output value 8B + script length 1B + script 25B (always)
        private const val OUTPUT_SIZE = 8 + 1 + 25

        // This input value is used to count core value without witness
        private const val MAX_SEGWIT_COMPAT_INPUT_SIZE = 32 + 4 + 1 + 46 + 4
        // This input value is used to count core value without witness
        private const val MAX_SEGWIT_NATIVE_INPUT_SIZE = 32 + 4 + 1 + 10 + 4
        private const val SEGWIT_COMPAT_OUTPUT_SIZE = 32
        private const val SEGWIT_NATIVE_OUTPUT_SIZE = 31
    }
}
