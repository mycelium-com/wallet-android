package com.mrd.bitlib

import com.mrd.bitlib.crypto.IPublicKeyRing
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.*
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.io.Serializable

open class UnsignedTransaction constructor(
        outputs: List<TransactionOutput>,
        funding: List<UnspentTransactionOutput>,
        keyRing: IPublicKeyRing,
        private val network: NetworkParameters,
        val lockTime: Int = 0,
        val defaultSequenceNumber: Int = NO_SEQUENCE
) : Serializable {
    val outputs = outputs.toTypedArray()
    val fundingOutputs = funding.toTypedArray()
    val signingRequests: Array<SigningRequest?> = arrayOfNulls(fundingOutputs.size)
    val inputs = fundingOutputs.map {
        TransactionInput(it.outPoint, ScriptInput.EMPTY, defaultSequenceNumber, it.value)
    }.toTypedArray()

    init {
        // Create transaction with valid outputs and empty inputs
        val transaction = Transaction(1, inputs, this.outputs, lockTime)

        for (i in fundingOutputs.indices) {
            if (isSegWitOutput(i)) {
                inputs[i].script = ScriptInput.fromOutputScript(funding[i].script)
            }
            val utxo = fundingOutputs[i]

            // Make sure that we only work on supported scripts
            when (utxo.script.javaClass) {
                !in SUPPORTED_SCRIPTS -> throw RuntimeException("Unsupported script")
            }

            // Find the address of the funding
            val address = utxo.script.getAddress(network)

            // Find the key to sign with
            val publicKey = keyRing.findPublicKeyByAddress(address)
                    ?: // This should not happen as we only work on outputs that we have
                    // keys for
                    throw RuntimeException("Public key not found")

            when (utxo.script) {
                is ScriptOutputP2SH  -> {
                    val inpScriptBytes = BitUtils.concatenate(byteArrayOf(Script.OP_0.toByte(), publicKey.pubKeyHashCompressed.size.toByte()), publicKey.pubKeyHashCompressed)
                    val inputScript = ScriptInput.fromScriptBytes(BitUtils.concatenate(byteArrayOf((inpScriptBytes.size and 0xFF).toByte()), inpScriptBytes))
                    transaction.inputs[i].script = inputScript
                    inputs[i].script = inputScript
                }
                is ScriptOutputP2WPKH -> {
                    val inpScriptBytes = BitUtils.concatenate(byteArrayOf(Script.OP_0.toByte(), publicKey.pubKeyHashCompressed.size.toByte()), publicKey.pubKeyHashCompressed)
                    val inputScript = ScriptInput.fromScriptBytes(BitUtils.concatenate(byteArrayOf((inpScriptBytes.size and 0xFF).toByte()), inpScriptBytes))
                    transaction.inputs[i].script = inputScript
                    inputs[i].script = inputScript
                }
            }

            val scriptsList: MutableList<ScriptInput> = mutableListOf()
            if (!isSegWitOutput(i)) {
                inputs.forEach {
                    scriptsList.add(it.script)
                    it.script = ScriptInput.EMPTY
                }
                inputs[i].script = ScriptInput.fromOutputScript(funding[i].script)
            }

            // Calculate the transaction hash that has to be signed
            val hash = transaction.getTxDigestHash(i)
            // Set the input to the empty script again
            if (!isSegWitOutput(i)) {
                inputs.forEachIndexed { index, it ->
                    it.script = scriptsList[index]
                }
                inputs[i] = TransactionInput(fundingOutputs[i].outPoint, ScriptInput.EMPTY, NO_SEQUENCE, fundingOutputs[i].value)
            }

            signingRequests[i] = SigningRequest(publicKey, hash)
        }
    }

    private fun isSegwitOutputScript(script: ScriptOutput) =
        script is ScriptOutputP2WPKH || script is ScriptOutputP2SH

    private fun isSegWitOutput(i: Int) =
            isSegwitOutputScript(fundingOutputs[i].script)

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

    fun getSignatureInfo(): Array<SigningRequest?> {
        return signingRequests
    }

    private fun getValue(value: Long?): String {
        return String.format("(%s)", CoinUtil.valueString(value!!, false))
    }

    companion object {
        private const val serialVersionUID = 1L
        const val NO_SEQUENCE = -1
        private val SUPPORTED_SCRIPTS = listOf(
                ScriptOutputStandard::class.java,
                ScriptOutputP2SH::class.java,
                ScriptOutputP2WPKH::class.java
        )
    }

}
