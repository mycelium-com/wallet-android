package com.mrd.bitlib

import com.mrd.bitlib.crypto.IPublicKeyRing
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.ScriptInput
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.util.CoinUtil

import java.io.Serializable

open class UnsignedTransaction(outputs: List<TransactionOutput>, funding: List<UnspentTransactionOutput>,
                               keyRing: IPublicKeyRing, private val network: NetworkParameters) : Serializable {

    val outputs = outputs.toTypedArray()
    val fundingOutputs = funding.toTypedArray()
    val signingRequests: Array<SigningRequest?> = arrayOfNulls(fundingOutputs.size)

    open val lockTime = 0
    open val defaultSequenceNumber = NO_SEQUENCE

    init {
        // Create empty input scripts pointing at the right out points
        val inputs = fundingOutputs.map {
            TransactionInput(it.outPoint, ScriptInput.EMPTY, defaultSequenceNumber)
        }.toTypedArray()

        // Create transaction with valid outputs and empty inputs
        val transaction = Transaction(1, inputs, this.outputs, lockTime, false)

        for (i in fundingOutputs.indices) {
            val utxo = fundingOutputs[i]

            // Make sure that we only work on standard output scripts
//            if (utxo.script !is ScriptOutputStandard) {
//                throw RuntimeException("Unsupported script")
//            }
            // TODO EVALUATE

            // Find the address of the funding
            val address = utxo.script.getAddress(network)

            // Find the key to sign with
            val publicKey = keyRing.findPublicKeyByAddress(address)
                    ?: // This should not happen as we only work on outputs that we have
                    // keys for
                    throw RuntimeException("Public key not found")

            // Set the input script to the funding output script
            inputs[i].script = ScriptInput.fromOutputScript(fundingOutputs[i].script)

            // Calculate the transaction hash that has to be signed
            val hash = StandardTransactionBuilder.hashTransaction(transaction)

            // Set the input to the empty script again
            inputs[i] = TransactionInput(fundingOutputs[i].outPoint, ScriptInput.EMPTY)

            signingRequests[i] = SigningRequest(publicKey, hash)
        }
    }

    /**
     * @return fee in satoshis
     */
    fun calculateFee(): Long {
        var `in`: Long = 0
        var out: Long = 0
        for (funding in fundingOutputs) {
            `in` += funding.value
        }
        for (output in outputs) {
            out += output.value
        }
        return `in` - out
    }

    override fun toString(): String {
        val sb = StringBuilder()
        val fee = CoinUtil.valueString(calculateFee(), false)
        sb.append(String.format("Fee: %s", fee)).append('\n')
        val max = Math.max(fundingOutputs.size, outputs.size)
        for (i in 0 until max) {
            val `in` = if (i < fundingOutputs.size) fundingOutputs[i] else null
            val out = if (i < outputs.size) outputs[i] else null
            val line: String
            line = if (`in` != null && out != null) {
                String.format("%36s %13s -> %36s %13s", `in`.script.getAddress(network), getValue(`in`.value),
                        out.script.getAddress(network), getValue(out.value))
            } else if (`in` != null) {
                String.format("%36s %13s    %36s %13s", `in`.script.getAddress(network), getValue(`in`.value), "",
                        "")
            } else if (out != null) {
                String.format("%36s %13s    %36s %13s", "", "", out.script.getAddress(network),
                        getValue(out.value))
            } else {
                ""
            }
            sb.append(line).append('\n')
        }
        return sb.toString()
    }

    private fun getValue(value: Long?): String {
        return String.format("(%s)", CoinUtil.valueString(value!!, false))
    }

    companion object {
        private const val serialVersionUID = 1L
        private const val NO_SEQUENCE = -1
    }

}
