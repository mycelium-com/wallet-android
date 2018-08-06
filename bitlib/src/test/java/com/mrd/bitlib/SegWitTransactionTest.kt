package com.mrd.bitlib

import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.HexUtils.toHex
import org.junit.Assert
import org.junit.Test

class SegWitTransactionTest {
    companion object {
        private const val P2SH_P2WPKH_Unconfirmed = "0100000001db6b1b20aa0fd7b23880be2ecbd4a98130974cf4748fb66092ac4d3ceb1a54770100000000feffffff02b8b4eb0b000000001976a914a457b684d7f0d539a46a45bbc043f35b59d0d96388ac0008af2f000000001976a914fd270b1ee6abcaea97fea7ad0402e8bd8ad6d77c88ac92040000"
        private const val P2SH_P2WPKH_SerializedSigned = "01000000000101db6b1b20aa0fd7b23880be2ecbd4a98130974cf4748fb66092ac4d3ceb1a5477010000001716001479091972186c449eb1ded22b78e40d009bdf0089feffffff02b8b4eb0b000000001976a914a457b684d7f0d539a46a45bbc043f35b59d0d96388ac0008af2f000000001976a914fd270b1ee6abcaea97fea7ad0402e8bd8ad6d77c88ac02473044022047ac8e878352d3ebbde1c94ce3a10d057c24175747116f8288e5d794d12d482f0220217f36a485cae903c713331d877c1f64677e3622ad4010726870540656fe9dcb012103ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a2687392040000"
        private const val P2SH_P2WPKH_HashPreimage = "0100000096b827c8483d4e9b96712b6713a7b68d6e8003a781feba36c31143470b4efd3752b0a642eea2fb7ae638c36f6252b6750293dbe574a806984b8e4d8548339a3bef51e1b804cc89d182d279655c3aa89e815b1b309fe287d9b2b55d57b90ec68a010000001976a9141d0f172a0ecb48aee1be1f2687d2963ae33f71a188ac0046c32300000000ffffffff863ef3e1a92afbfdb97f31ad0fc7683ee943e9abcf2501590ff8f6551f47e5e51100000001000000"
    }

    @Test
    @Throws(Transaction.TransactionParsingException::class)
    fun generateSignaturesSegwit() {
        val publicKey = PublicKey(HexUtils.toBytes("03ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a26873"))
        val privateKey = InMemoryPrivateKey(HexUtils.toBytes("eb696a065ef48a2192da5b28b694f87544b30fae8327c4510137a922f32c6dcf"))
        val publicKeyRing = IPublicKeyRing { _ ->
            publicKey
        }
        val privateKeyRing = IPrivateKeyRing { _ ->
            privateKey
        }
        val tx = Transaction.fromBytes(HexUtils.toBytes(P2SH_P2WPKH_Unconfirmed))
//        Assert.assertEquals(P2SH_P2WPKH_Unconfirmed, toHex(tx.toBytes()))

        val input1 = UnspentTransactionOutput(tx.inputs[0].outPoint, 10,1000000000, ScriptOutput.fromScriptBytes(HexUtils.toBytes("a9144733f37cf4db86fbc2efed2500b4f4e49f31202387")), true)
        val toBytes = HexUtils.toBytes("001479091972186c449eb1ded22b78e40d009bdf0089")
        //redeem script is OP_0 -> pubkeyhash
        val unsignedTransaction =  UnsignedTransaction(tx.outputs.asList(), arrayListOf(input1), publicKeyRing, NetworkParameters.productionNetwork, true, tx.lockTime, tx.inputs[0].sequence)
        val unsignedBytes = Transaction.fromUnsignedTransaction(unsignedTransaction).toBytes()

        //print(unsignedTransaction.signingRequests[0]!!.toSign.bytes.map { it.toString() })
        //Assert.assertEquals(P2SH_P2WPKH_HashPreimage, HexUtils.toHex(unsignedTransaction.signingRequests[0]!!.toSign.bytes))
        val signatures = StandardTransactionBuilder.generateSignatures(
                unsignedTransaction.signingRequests,
                privateKeyRing
        )


        val finalTx = StandardTransactionBuilder.finalizeTransaction(unsignedTransaction, signatures)
        Assert.assertEquals(P2SH_P2WPKH_SerializedSigned, toHex(finalTx.toBytes()))
    }
}