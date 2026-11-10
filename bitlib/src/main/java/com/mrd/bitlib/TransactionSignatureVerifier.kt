package com.mrd.bitlib

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.schnorr.SchnorrVerify
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.Script
import com.mrd.bitlib.model.ScriptInput
import com.mrd.bitlib.model.ScriptInputP2TR
import com.mrd.bitlib.model.ScriptInputP2WPKH
import com.mrd.bitlib.model.ScriptOutputP2TR
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.model.signature.TaprootCommonSignatureMessageBuilder
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mrd.bitlib.util.TaprootUtils.Companion.sigHash

/**
 * Checks that an input of a transaction carries a valid signature by the key of the output it
 * spends — for the script types our accounts own: P2PKH, P2SH-P2WPKH, P2WPKH and P2TR key path,
 * with SIGHASH_ALL (or the Taproot default). Anything else counts as not verified.
 *
 * A server can hand us any bytes as "a transaction spending your coin". Without this check such a
 * forgery became our own outgoing transaction (Electrum GHSA-6jp4-8v48-cc5v).
 */
object TransactionSignatureVerifier {
    /**
     * @param spent the outputs the inputs of [tx] spend, by outpoint. Taproot signs all of them,
     * so a P2TR input only verifies when every one is known.
     */
    @JvmStatic
    fun verifyInput(tx: BitcoinTransaction, index: Int, spent: (OutPoint) -> UnspentTransactionOutput?): Boolean =
        try {
            val funding = spent(tx.inputs[index].outPoint)
            when {
                funding == null -> false
                funding.script is ScriptOutputP2TR -> verifyTaproot(tx, index, funding, spent)
                tx.inputs[index].witness.stackSize == 2 -> verifySegwitV0(tx, index, funding)
                else -> verifyLegacy(tx, index, funding)
            }
        } catch (e: Exception) {
            // Malformed scripts, keys or signatures: nothing we could have signed
            false
        }

    private fun verifyLegacy(tx: BitcoinTransaction, index: Int, funding: UnspentTransactionOutput): Boolean {
        val chunks = Script.chunksFromScriptBytes(tx.inputs[index].script.scriptBytes)
        if (chunks.size != 2) return false
        val (signature, publicKey) = chunks
        if (!funding.script.scriptBytes.contentEquals(p2pkhScript(HashUtils.addressHash(publicKey)))) return false
        // Legacy sighash: every input script blank but ours, which carries the script it spends
        val inputs = tx.inputs.mapIndexed { i, input ->
            TransactionInput(
                input.outPoint,
                if (i == index) ScriptInput.fromOutputScript(funding.script) else ScriptInput.EMPTY,
                input.sequence,
                0
            )
        }
        val digest = BitcoinTransaction(tx.version, inputs.toTypedArray(), tx.outputs, tx.lockTime).getTxDigestHash(index)
        return verifyEcdsa(signature, publicKey, digest)
    }

    private fun verifySegwitV0(tx: BitcoinTransaction, index: Int, funding: UnspentTransactionOutput): Boolean {
        val witness = tx.inputs[index].witness
        val signature = witness.getStack(0)
        val publicKey = witness.getStack(1)
        val keyHash = HashUtils.addressHash(publicKey)
        val program = byteArrayOf(Script.OP_0.toByte(), keyHash.size.toByte()) + keyHash
        val script = funding.script.scriptBytes
        val nested = script.contentEquals(p2shScript(HashUtils.addressHash(program)))
        if (!nested && !script.contentEquals(program)) return false
        // BIP143 takes the spent value and the P2PKH script code of the key hash
        val inputs = tx.inputs.mapIndexed { i, input ->
            TransactionInput(
                input.outPoint,
                if (i == index) ScriptInputP2WPKH(byteArrayOf(program.size.toByte()) + program) else ScriptInput.EMPTY,
                input.sequence,
                if (i == index) funding.value else 0
            )
        }
        val digest = BitcoinTransaction(tx.version, inputs.toTypedArray(), tx.outputs, tx.lockTime).getTxDigestHash(index)
        return verifyEcdsa(signature, publicKey, digest)
    }

    private fun verifyTaproot(
        tx: BitcoinTransaction,
        index: Int,
        funding: UnspentTransactionOutput,
        spent: (OutPoint) -> UnspentTransactionOutput?
    ): Boolean {
        val witness = tx.inputs[index].witness
        // Key path only: a single signature, no annex, no script
        if (witness.stackSize != 1) return false
        val signature = witness.getStack(0)
        val hashType = when (signature.size) {
            64 -> Script.SIGHASH_DEFAULT
            65 -> signature[64].toInt().takeIf { it == Script.SIGHASH_ALL } ?: return false
            else -> return false
        }
        val utxos = tx.inputs.map { spent(it.outPoint) ?: return false }
        val inputs = tx.inputs.mapIndexed { i, input ->
            TransactionInput(input.outPoint, ScriptInputP2TR(utxos[i].script.scriptBytes), input.sequence, utxos[i].value)
        }
        val message = TaprootCommonSignatureMessageBuilder(
            BitcoinTransaction(tx.version, inputs.toTypedArray(), tx.outputs, tx.lockTime), utxos.toTypedArray(), index, tx.version
        ).apply { this.hashType = hashType }.build().toBytes()
        // The output key itself: 51 20 <x-only key>
        val outputKey = funding.script.scriptBytes.copyOfRange(2, 34)
        return SchnorrVerify(outputKey).verify(signature.copyOfRange(0, 64), sigHash(message).bytes)
    }

    private fun verifyEcdsa(signature: ByteArray, publicKey: ByteArray, digest: Sha256Hash): Boolean {
        if (signature.isEmpty() || signature.last().toInt() != Script.SIGHASH_ALL) return false
        return PublicKey(publicKey).verifyDerEncodedSignature(digest, signature.copyOfRange(0, signature.size - 1))
    }

    private fun p2pkhScript(keyHash: ByteArray) =
        byteArrayOf(Script.OP_DUP.toByte(), Script.OP_HASH160.toByte(), keyHash.size.toByte()) + keyHash +
            byteArrayOf(Script.OP_EQUALVERIFY.toByte(), Script.OP_CHECKSIG.toByte())

    private fun p2shScript(scriptHash: ByteArray) =
        byteArrayOf(Script.OP_HASH160.toByte(), scriptHash.size.toByte()) + scriptHash + byteArrayOf(Script.OP_EQUAL.toByte())
}
