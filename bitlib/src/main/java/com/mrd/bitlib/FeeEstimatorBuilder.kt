package com.mrd.bitlib

import com.mrd.bitlib.model.*

class FeeEstimatorBuilder {
    private var legacyInputs: Int = 0
    private var p2shSegwitInputs: Int = 0
    private var bechInputs: Int = 0
    private var legacyOutputs: Int = 0
    private var p2shOutputs: Int = 0
    private var bechOutputs: Int = 0
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

    fun setLegacyOutputs(legacyOutputs: Int): FeeEstimatorBuilder {
        this.legacyOutputs = legacyOutputs
        return this
    }

    fun addOutput(addressType: AddressType): FeeEstimatorBuilder {
        when (addressType) {
            AddressType.P2PKH -> legacyOutputs++
            AddressType.P2WPKH -> bechOutputs++
            AddressType.P2SH_P2WPKH -> p2shOutputs++
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

    fun setArrayOfInputs(inputsList: Iterable<UnspentTransactionOutput>) =
            setArrayOfInputs(inputsList.toList().toTypedArray())

    fun setArrayOfInputs(inputsArray: Array<UnspentTransactionOutput>): FeeEstimatorBuilder {
        legacyInputs = inputsArray.filter { it.script is ScriptOutputP2PKH }.count()
        p2shSegwitInputs = inputsArray.filter { it.script is ScriptOutputP2SH }.count()
        bechInputs = inputsArray.filter { it.script is ScriptOutputP2WPKH }.count()
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
            legacyOutputs = outputsArray.filter { it.script is ScriptOutputP2PKH }.count()
            p2shOutputs = outputsArray.filter { it.script is ScriptOutputP2SH }.count()
            bechOutputs = outputsArray.filter { it.script is ScriptOutputP2WPKH }.count()
        } else {
            legacyOutputs = 2
        }
        return this
    }

    fun setMinerFeePerKb(minerFeePerKb: Long): FeeEstimatorBuilder {
        this.minerFeePerKb = minerFeePerKb
        return this
    }

    fun createFeeEstimator(): FeeEstimator {
        return FeeEstimator(legacyInputs, p2shSegwitInputs, bechInputs, legacyOutputs, p2shOutputs,
                bechOutputs, minerFeePerKb)
    }
}