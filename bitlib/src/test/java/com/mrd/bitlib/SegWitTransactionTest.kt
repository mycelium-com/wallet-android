package com.mrd.bitlib

import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.HexUtils.toBytes
import com.mrd.bitlib.util.HexUtils.toHex
import org.junit.Assert
import org.junit.Test

class SegWitTransactionTest {
    companion object {
        private const val P2SH_P2WPKH_UNSIGNED = "0100000001db6b1b20aa0fd7b23880be2ecbd4a98130974cf4748fb66092ac4d3ceb1a54770100000000feffffff02b8b4eb0b000000001976a914a457b684d7f0d539a46a45bbc043f35b59d0d96388ac0008af2f000000001976a914fd270b1ee6abcaea97fea7ad0402e8bd8ad6d77c88ac92040000"
        private const val P2SH_P2WPKH_SIGNED = "01000000000101db6b1b20aa0fd7b23880be2ecbd4a98130974cf4748fb66092ac4d3ceb1a5477010000001716001479091972186c449eb1ded22b78e40d009bdf0089feffffff02b8b4eb0b000000001976a914a457b684d7f0d539a46a45bbc043f35b59d0d96388ac0008af2f000000001976a914fd270b1ee6abcaea97fea7ad0402e8bd8ad6d77c88ac02473044022047ac8e878352d3ebbde1c94ce3a10d057c24175747116f8288e5d794d12d482f0220217f36a485cae903c713331d877c1f64677e3622ad4010726870540656fe9dcb012103ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a2687392040000"
    }

    @Test
    @Throws(Transaction.TransactionParsingException::class)
    fun generateSignaturesP2SH_P2WPKH() {
        val publicKey = PublicKey(toBytes("03ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a26873"))
        val privateKey = InMemoryPrivateKey(toBytes("eb696a065ef48a2192da5b28b694f87544b30fae8327c4510137a922f32c6dcf"))
        val publicKeyRing = IPublicKeyRing { publicKey }
        val privateKeyRing = IPrivateKeyRing { privateKey }
        val tx = Transaction.fromBytes(toBytes(P2SH_P2WPKH_UNSIGNED))
        Assert.assertEquals(P2SH_P2WPKH_UNSIGNED, toHex(tx.toBytes()))

        val input1 = UnspentTransactionOutput(tx.inputs[0].outPoint, 10,1000000000,
                ScriptOutput.fromScriptBytes(toBytes("a9144733f37cf4db86fbc2efed2500b4f4e49f31202387")))
        val unsignedTransaction = UnsignedTransaction(tx.outputs.asList(), arrayListOf(input1), publicKeyRing,
                NetworkParameters.productionNetwork, tx.lockTime, tx.inputs[0].sequence)

        val signatures = StandardTransactionBuilder.generateSignatures(
                unsignedTransaction.signingRequests,
                privateKeyRing
        )
        val finalTx = StandardTransactionBuilder.finalizeTransaction(unsignedTransaction, signatures)
        Assert.assertEquals(P2SH_P2WPKH_SIGNED, toHex(finalTx.toBytes()))
    }
}