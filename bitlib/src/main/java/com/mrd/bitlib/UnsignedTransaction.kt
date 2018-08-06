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
        val isSegwit: Boolean,
        val lockTime: Int = 0,
        val defaultSequenceNumber: Int = NO_SEQUENCE
) : Serializable {

    constructor(outputs: List<TransactionOutput>,
                funding: List<UnspentTransactionOutput>,
                keyRing: IPublicKeyRing,
                network: NetworkParameters
    ) : this(outputs, funding, keyRing, network, false, 0, NO_SEQUENCE)

    val outputs = outputs.toTypedArray()
    val fundingOutputs = funding.toTypedArray()
    val signingRequests: Array<SigningRequest?> = arrayOfNulls(fundingOutputs.size)
    val inputs = fundingOutputs.map {
        TransactionInput(it.outPoint, ScriptInput.fromOutputScript(it.script), defaultSequenceNumber, it.value)
    }.toTypedArray()

    init {
        // Create empty input scripts pointing at the right out points


        // Create transaction with valid outputs and empty inputs
        val transaction = Transaction(1, inputs, this.outputs, lockTime, isSegwit)

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

            if (utxo.script is ScriptOutputP2SH) {
                val inpScriptBytes = BitUtils.concatenate(byteArrayOf(Script.OP_0.toByte(), publicKey.pubKeyHashCompressed.size.toByte()), publicKey.pubKeyHashCompressed)
                val inputScript = ScriptInput.fromScriptBytes(BitUtils.concatenate(byteArrayOf((inpScriptBytes.size and 0xFF).toByte()), inpScriptBytes))
                transaction.inputs[i].script = inputScript
            } else if (utxo.script is ScriptOutputP2WPKH) {
                throw NotImplementedError()
            } else if (utxo.script is ScriptOutputP2WSH) {
                throw NotImplementedError()
            }
            // Calculate the transaction hash that has to be signed
            val hash = getTxDigestHash(transaction, i)
            // Set the input to the empty script again
            //inputs[i] = TransactionInput(fundingOutputs[i].outPoint, ScriptInput.EMPTY, NO_SEQUENCE, fundingOutputs[i].value)

            signingRequests[i] = SigningRequest(publicKey, hash)
        }
    }

    private fun getTxDigestHash(transaction: Transaction, i: Int): Sha256Hash {
        val writer = ByteWriter(1024)
        if (transaction.isSegwit) {
            writer.putIntLE(transaction.version)
            writer.putBytes(transaction.getPrevOutsHash().bytes)
            writer.putBytes(transaction.getSequenceHash().bytes)
            transaction.inputs[i].outPoint.hashPrevOutToByteWriter(writer)
            val scriptCode = transaction.inputs[i].getScriptCode()
            writer.put((scriptCode.size and 0xFF).toByte())
            writer.putBytes(scriptCode)
            writer.putLongLE(transaction.inputs[i].value)
            writer.putIntLE(transaction.inputs[i].sequence)
            writer.putBytes(transaction.getOutputsHash().bytes)
            writer.putIntLE(lockTime)
            val hashType = 1
            writer.putIntLE(hashType)
        } else {
            transaction.toByteWriter(writer)
            // We also have to write a hash type.
            val hashType = 1
            writer.putIntLE(hashType)
            // Note that this is NOT reversed to ensure it will be signed
            // correctly. If it were to be printed out
            // however then we would expect that it is IS reversed.
        }
        return HashUtils.doubleSha256(writer.toBytes())
    }

    private fun Transaction.getPrevOutsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        for (input in inputs) {
            input.outPoint.hashPrevOutToByteWriter(writer)
        }
        return HashUtils.doubleSha256(writer.toBytes())
    }

    private fun Transaction.getOutputsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        for (output in outputs) {
            writer.putLongLE(output.value)
            writer.put((output.script.scriptBytes.size and 0xFF).toByte())
            writer.putBytes(output.script.scriptBytes)
        }
        return HashUtils.doubleSha256(writer.toBytes())
    }

    private fun Transaction.getSequenceHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        for (input in inputs) {
            writer.putIntLE(input.sequence)
        }
        return HashUtils.doubleSha256(writer.toBytes())
    }

    private fun TransactionInput.getScriptCode(): ByteArray {
        val byteWriter = ByteWriter(1024)
        if (script is ScriptOutputP2SH) {
            throw NotImplementedException()
        } else if (script is ScriptInputP2WSH) {
            throw NotImplementedException()
        } else if (script is ScriptInputP2WPKH) {
            byteWriter.put(Script.OP_DUP.toByte())
            byteWriter.put(Script.OP_HASH160.toByte())
            val witnessProgram = ScriptInput.getWitnessProgram(ScriptInput.depush(script.scriptBytes))
            byteWriter.put((0xFF and witnessProgram.size).toByte())
            byteWriter.putBytes(witnessProgram)
            byteWriter.put(Script.OP_EQUALVERIFY.toByte())
            byteWriter.put(Script.OP_CHECKSIG.toByte())
        } else {
            throw IllegalArgumentException("No scriptcode for " + script!!.javaClass.canonicalName)
        }
        return byteWriter.toBytes()
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

    fun getSignatureInfo(): Array<SigningRequest?> {
        return signingRequests
    }

    private fun getValue(value: Long?): String {
        return String.format("(%s)", CoinUtil.valueString(value!!, false))
    }

    companion object {
        private const val serialVersionUID = 1L
        const val NO_SEQUENCE = -1
    }

}
