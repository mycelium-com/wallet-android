package com.mrd.bitlib.model.signature

import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash

class WitnessSignatureMessageBuilder(
    val tx: BitcoinTransaction,
    val index: Int,
    val version: Int
) {
    val inputs = tx.inputs
    val outputs = tx.outputs
    val lockTime = tx.lockTime

    private fun getPrevOutsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        inputs.forEach { input ->
            input.outPoint.hashPrev(writer)
        }
        return HashUtils.doubleSha256(writer.toBytes());
    }

    private fun getSequenceHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        inputs.forEach { input ->
            writer.putIntLE(input.sequence);
        }
        return HashUtils.doubleSha256(writer.toBytes());
    }

    private fun getOutputsHash(): Sha256Hash {
        val writer = ByteWriter(1024)
        outputs.forEach { output ->
            writer.putLongLE(output.value);
            writer.put((output.script.scriptBytes.size and 0xFF).toByte())
            writer.putBytes(output.script.scriptBytes);
        }
        return HashUtils.doubleSha256(writer.toBytes());
    }

    fun build(writer: ByteWriter) {
        writer.putIntLE(version)
        writer.putSha256Hash(getPrevOutsHash())
        writer.putSha256Hash(getSequenceHash())
        inputs[index].outPoint.hashPrev(writer)
        val scriptCode = inputs[index].getScriptCode()
        writer.put((scriptCode.size and 0xFF).toByte())
        writer.putBytes(scriptCode)
        writer.putLongLE(inputs[index].getValue())
        writer.putIntLE(inputs[index].sequence)
        writer.putSha256Hash(getOutputsHash())
        writer.putIntLE(lockTime)
    }
}