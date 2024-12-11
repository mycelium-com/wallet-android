package com.mrd.bitlib.model.signature

import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.Script
import com.mrd.bitlib.model.Script.SIGHASH_ANYONECANPAY
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash

class TaprootCommonSignatureMessageBuilder(
    val tx: BitcoinTransaction,
    val utxos: Array<UnspentTransactionOutput>,
    val index: Int,
    val version: Int
) {
    val inputs = tx.inputs
    val outputs = tx.outputs
    val lockTime = tx.lockTime
    var hashType = Script.SIGHASH_ALL

    /**
     * Hash Prevouts(32 bytes) = txid+vout
     */
    private fun prevOutputsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        inputs.forEach { input ->
            input.outPoint.hashPrev(writer)

        }
        return hash(writer.toBytes())
    }

    /**
     * Hash Amounts(32 bytes) = amount
     */
    private fun inputAmountsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        inputs.forEach { input ->
            writer.putLongLE(input.value)
        }
        return hash(writer.toBytes())
    }

    /**
     * Hash ScriptPubKeys 32 bytes = scriptpubkeysize + scriptpubkey
     */
    private fun scriptPubKeysHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        inputs.forEach { input ->
            val utxo = utxos.find { it.outPoint == input.outPoint }
            val scriptBytes = utxo?.script?.scriptBytes ?: throw RuntimeException("Script is null")
            writer.put((scriptBytes.size and 0xFF).toByte())
            writer.putBytes(scriptBytes)
        }
        return hash(writer.toBytes())
    }


    /**
     * Hash Sequences(32 bytes) = sequence
     */
    private fun sequenceHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        inputs.forEach { input ->
            writer.putIntLE(input.sequence)
        }
        return hash(writer.toBytes())
    }

    /**
     * Hash Outputs 32 bytes = amount + scriptpubkeysize + scriptpubkey
     */
    private fun outputsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        outputs.forEach { output ->
            writer.putLongLE(output.value)
            writer.put((output.script.scriptBytes.size and 0xFF).toByte())
            writer.putBytes(output.script.scriptBytes)
        }
        return hash(writer.toBytes());
    }

    /**
     * Hash Single Output 32 bytes = amount + scriptpubkeysize + scriptpubkey
     * work with SIGHASH_SINGLE
     */

    private fun outputHash(index: Int): Sha256Hash {
        val writer = ByteWriter(1024)
        outputs[index].let { output ->
            writer.putLongLE(output.value)
            writer.put((output.script.scriptBytes.size and 0xFF).toByte())
            writer.putBytes(output.script.scriptBytes)
        }
        return hash(writer.toBytes());
    }


    private fun hash(data: ByteArray): Sha256Hash = HashUtils.sha256(data)

    fun build(writer: ByteWriter = ByteWriter(1024)): ByteWriter {
        writer.put(hashType.toByte())     //hash type byte
        writer.putIntLE(version)                    //tx version
        writer.putIntLE(lockTime)                   //locktime
        val isAnyOneCanPay = isAnyOneCanPay()
        if (!isAnyOneCanPay) {
            writer.putSha256Hash(prevOutputsHash())     //hash prevouts
            writer.putSha256Hash(inputAmountsHash())    //hash amounts
            writer.putSha256Hash(scriptPubKeysHash())   //hash scriptpubkeys
            writer.putSha256Hash(sequenceHash())        //hash sequences
        }
        if (hashType and 3 < 2) {
            writer.putSha256Hash(outputsHash())         //hash outputs
        }
        writer.put(0)                     //spend type 0 - key 1 - taproot script
        if (!isAnyOneCanPay) {
            writer.putIntLE(index)                  //input index
        }
        if (isAnyOneCanPay) {
            inputs[index].outPoint.hashPrev(writer)
            writer.putLongLE(inputs[index].value)
            writer.put((inputs[index].script.scriptBytes.size and 0xFF).toByte())
            writer.putBytes(inputs[index].script.scriptBytes)
            writer.putIntLE(inputs[index].sequence)
        }
        if (hashType and 3 == 3) {
            writer.putSha256Hash(outputHash(index))
        }
        return writer
    }

    private fun isAnyOneCanPay(): Boolean = hashType and SIGHASH_ANYONECANPAY != 0
}