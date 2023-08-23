package com.mrd.bitlib

import com.mrd.bitlib.model.*

class FeeEstimatorBuilder {
    private var legacyInputs: Int = 0
    private var p2shSegwitInputs: Int = 0
    private var bechInputs: Int = 0
    private var bech32mInputs: Int = 0
    private var legacyOutputs: Int = 0
    private var p2shOutputs: Int = 0
    private var bechOutputs: Int = 0
    private var bech32mOutputs: Int = 0
    private var minerFeePerKb: Long = 0

    fun setLegacyInputs(legacyInputs: Int): FeeEstimatorBuilder {
        this.legacyInputs = legacyInputs
        return this
    }

    fun setP2shSegwitInputs(p2shSegwitInputs: Int): FeeEstimatorBuilder {
        this.p2shSegwitInputs = p2shSegwitInputs
        return this
    }

    fun setBechInputs(bechInputs: Int): FeeEstimatorBuilder {
        this.bechInputs = bechInputs
        return this
    }

    fun setBech32mInputs(inputs: Int): FeeEstimatorBuilder {
        this.bech32mInputs = inputs
        return this
    }

    fun setLegacyOutputs(legacyOutputs: Int): FeeEstimatorBuilder {
        this.legacyOutputs = legacyOutputs
        return this
    }

    fun addOutput(addressType: AddressType): FeeEstimatorBuilder {
        when (addressType) {
            AddressType.P2PKH -> legacyOutputs++
            AddressType.P2WPKH -> bechOutputs++
            AddressType.P2SH_P2WPKH -> p2shOutputs++
            AddressType.P2TR -> bech32mOutputs++
        }
        return this
    }

    fun setP2shOutputs(p2shOutputs: Int): FeeEstimatorBuilder {
        this.p2shOutputs = p2shOutputs
        return this
    }

    fun setBechOutputs(bechOutputs: Int): FeeEstimatorBuilder {
        this.bechOutputs = bechOutputs
        return this
    }

    fun setBech32mOutputs(outputs: Int): FeeEstimatorBuilder {
        this.bech32mOutputs = outputs
        return this
    }

    fun setArrayOfInputs(inputsList: Iterable<UnspentTransactionOutput>) =
            setArrayOfInputs(inputsList.toList().toTypedArray())

    fun setArrayOfInputs(inputsArray: Array<UnspentTransactionOutput>): FeeEstimatorBuilder {
        legacyInputs = inputsArray.count { it.script is ScriptOutputP2PKH }
        p2shSegwitInputs = inputsArray.count { it.script is ScriptOutputP2SH }
        bechInputs = inputsArray.count { it.script is ScriptOutputP2WPKH }
        bech32mInputs = inputsArray.count { it.script is ScriptOutputP2TR }
        return this
    }

    fun setArrayOfOutputs(inputsList: Iterable<TransactionOutput>) =
            setArrayOfOutputs(inputsList.toList().toTypedArray())

    /**
     * This method is used to set outputs for fee calculation.
     * @param outputsArray is nullable. If it's null 2 legacy outputs used as this is biggest possible fee, which
     * should be calculated by default for our wallet.
     */
    fun setArrayOfOutputs(outputsArray: Array<TransactionOutput>?): FeeEstimatorBuilder {
        if (outputsArray != null) {
            legacyOutputs = outputsArray.count { it.script is ScriptOutputP2PKH }
            p2shOutputs = outputsArray.count { it.script is ScriptOutputP2SH }
            bechOutputs = outputsArray.count { it.script is ScriptOutputP2WPKH }
            bech32mOutputs = outputsArray.count { it.script is ScriptOutputP2TR }
        } else {
            legacyOutputs = 2
        }
        return this
    }

    fun setMinerFeePerKb(minerFeePerKb: Long): FeeEstimatorBuilder {
        this.minerFeePerKb = minerFeePerKb
        return this
    }


    fun createFeeEstimator(): FeeEstimator =
            FeeEstimator(legacyInputs, p2shSegwitInputs, bechInputs, bech32mInputs,
                    legacyOutputs, p2shOutputs, bechOutputs, bech32mOutputs, minerFeePerKb)

    override fun toString(): String =
            "FeeEstimatorBuilder(legacyInputs=$legacyInputs, p2shSegwitInputs=$p2shSegwitInputs" +
                    ", bechInputs=$bechInputs, bech32mInputs=$bech32mInputs, legacyOutputs=$legacyOutputs, p2shOutputs=$p2shOutputs" +
                    ", bechOutputs=$bechOutputs, bech32mOutputs=${bech32mOutputs}, minerFeePerKb=$minerFeePerKb)"
}