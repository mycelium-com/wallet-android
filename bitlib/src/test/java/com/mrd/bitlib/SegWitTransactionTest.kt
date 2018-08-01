package com.mrd.bitlib

import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.ScriptOutput
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.HexUtils.toHex
import org.junit.Assert
import org.junit.Test

class SegWitTransactionTest {
    companion object {
        private const val P2SH_P2WPKH_Unconfirmed = "0100000001db6b1b20aa0fd7b23880be2ecbd4a98130974cf4748fb66092ac4d3ceb1a54770100000000feffffff02b8b4eb0b000000001976a914a457b684d7f0d539a46a45bbc043f35b59d0d96388ac0008af2f000000001976a914fd270b1ee6abcaea97fea7ad0402e8bd8ad6d77c88ac92040000"
        private const val P2SH_P2WPKH_SerializedSigned = "01000000000102fff7f7881a8099afa6940d42d1e7f6362bec38171ea3edf433541db4e4ad969f00000000494830450221008b9d1dc26ba6a9cb62127b02742fa9d754cd3bebf337f7a55d114c8e5cdd30be022040529b194ba3f9281a99f2b1c0a19c0489bc22ede944ccf4ecbab4cc618ef3ed01eeffffffef51e1b804cc89d182d279655c3aa89e815b1b309fe287d9b2b55d57b90ec68a0100000000ffffffff02202cb206000000001976a9148280b37df378db99f66f85c95a783a76ac7a6d5988ac9093510d000000001976a9143bde42dbee7e4dbe6a21b2d50ce2f0167faa815988ac000247304402203609e17b84f6a7d30c80bfa610b5b4542f32a8a0d5447a12fb1366d7f01cc44a0220573a954c4518331561406f90300e8f3358f51928d43c212a8caed02de67eebee0121025476c2e83188368da1ff3e292e7acafcdb3566bb0ad253f62fc70f07aeee635711000000"
        private const val P2SH_P2WPKH_HashPreimage = "0100000096b827c8483d4e9b96712b6713a7b68d6e8003a781feba36c31143470b4efd3752b0a642eea2fb7ae638c36f6252b6750293dbe574a806984b8e4d8548339a3bef51e1b804cc89d182d279655c3aa89e815b1b309fe287d9b2b55d57b90ec68a010000001976a9141d0f172a0ecb48aee1be1f2687d2963ae33f71a188ac0046c32300000000ffffffff863ef3e1a92afbfdb97f31ad0fc7683ee943e9abcf2501590ff8f6551f47e5e51100000001000000"
    }

    // timing out after 50 * 10 ms. 50 is the signature count, to average a bit,
    // 10ms is what it may take at max in the test per sig.
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
        Assert.assertEquals(P2SH_P2WPKH_Unconfirmed, toHex(tx.toBytes()))

        val input1 = UnspentTransactionOutput(tx.inputs[0].outPoint, 10,1000000000, ScriptOutput.fromScriptBytes(tx.inputs[0].script.scriptBytes), false)
        val unsignedTransaction = UnsignedTransaction(tx.outputs.asList(), arrayListOf(input1), publicKeyRing, NetworkParameters.productionNetwork, tx.lockTime, tx.inputs[0].sequence)
        val unsignedBytes = Transaction.fromUnsignedTransaction(unsignedTransaction).toBytes()
        Assert.assertEquals(P2SH_P2WPKH_Unconfirmed, toHex(unsignedBytes)) //TO THIS POINT EVERYTHING PRETTY good

        val signatures = StandardTransactionBuilder.generateSignatures(
                unsignedTransaction.signingRequests,
                privateKeyRing
        )

        Assert.assertEquals(P2SH_P2WPKH_HashPreimage, HexUtils.toHex(unsignedTransaction.signingRequests[0]!!.toSign.bytes))

        val finalTx = StandardTransactionBuilder.finalizeTransaction(unsignedTransaction, signatures)
        Assert.assertEquals(P2SH_P2WPKH_SerializedSigned, toHex(finalTx.toBytes()))
    }
}