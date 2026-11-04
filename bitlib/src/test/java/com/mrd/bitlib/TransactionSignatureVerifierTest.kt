package com.mrd.bitlib

import com.mrd.bitlib.crypto.BitcoinSigner
import com.mrd.bitlib.crypto.IPrivateKeyRing
import com.mrd.bitlib.crypto.IPublicKeyRing
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionSignatureVerifierTest {
    private val network = NetworkParameters.testNetwork
    private val keys = AddressType.values().mapIndexed { i, type ->
        type to InMemoryPrivateKey(HexUtils.toBytes("2$i" + "00".repeat(31)), true)
    }.toMap()
    private val addresses = keys.mapValues { (type, key) -> key.publicKey.toAddress(network, type) }
    private val stranger = InMemoryPrivateKey(HexUtils.toBytes("33" + "00".repeat(31)), true)
        .publicKey.toAddress(network, AddressType.P2WPKH)

    private val publicKeyRing = object : IPublicKeyRing {
        override fun findPublicKeyByAddress(address: BitcoinAddress?): PublicKey? =
            addresses.entries.firstOrNull { it.value == address }?.let { keys[it.key]!!.publicKey }
    }
    private val privateKeyRing = object : IPrivateKeyRing {
        override fun findSignerByPublicKey(publicKey: PublicKey?): BitcoinSigner? =
            keys.values.firstOrNull { it.publicKey == publicKey }
    }

    private fun utxo(type: AddressType, n: Int) = UnspentTransactionOutput(
        OutPoint(HashUtils.sha256(byteArrayOf(n.toByte())), n), 100, 100_000L + n,
        StandardTransactionBuilder.createOutput(addresses[type]!!, 100_000L + n, network).script
    )

    /** Signs a spend of [funding] the way our accounts do, and parses it back as a server would hand it over. */
    private fun signedSpend(funding: List<UnspentTransactionOutput>): BitcoinTransaction {
        val outputs = arrayOf(StandardTransactionBuilder.createOutput(stranger, 50_000, network))
        val unsigned = UnsignedTransaction(outputs.toList(), funding, publicKeyRing, network, 0, UnsignedTransaction.NO_SEQUENCE)
        val signatures = StandardTransactionBuilder.generateSignatures(unsigned.signingRequests.filterNotNull().toTypedArray(), privateKeyRing)
        return BitcoinTransaction.fromBytes(StandardTransactionBuilder.finalizeTransaction(unsigned, signatures).toBytes())
    }

    private fun verifiesAll(tx: BitcoinTransaction, funding: List<UnspentTransactionOutput>): Boolean {
        val spent = funding.associateBy { it.outPoint }
        return tx.inputs.indices.all { TransactionSignatureVerifier.verifyInput(tx, it) { o -> spent[o] } }
    }

    private fun withOutputValue(tx: BitcoinTransaction, value: Long) = BitcoinTransaction(
        tx.version, tx.inputs, arrayOf(TransactionOutput(value, tx.outputs[0].script)), tx.lockTime
    )

    @Test
    fun ourSignatureVerifiesForEveryAccountType() {
        AddressType.values().forEachIndexed { i, type ->
            val funding = listOf(utxo(type, i))
            assertTrue("$type", verifiesAll(signedSpend(funding), funding))
        }
    }

    @Test
    fun mixedInputsVerify() {
        val funding = AddressType.values().mapIndexed { i, type -> utxo(type, i) }
        assertTrue(verifiesAll(signedSpend(funding), funding))
    }

    @Test
    fun changedOutputFailsForEveryAccountType() {
        AddressType.values().forEachIndexed { i, type ->
            val funding = listOf(utxo(type, i))
            assertFalse("$type", verifiesAll(withOutputValue(signedSpend(funding), 49_000), funding))
        }
    }

    @Test
    fun wrongSpentValueFailsWhereTheSignatureCoversIt() {
        listOf(AddressType.P2SH_P2WPKH, AddressType.P2WPKH, AddressType.P2TR).forEachIndexed { i, type ->
            val funding = listOf(utxo(type, i))
            val lie = funding.map { UnspentTransactionOutput(it.outPoint, it.height, it.value - 1, it.script) }
            assertFalse("$type", verifiesAll(signedSpend(funding), lie))
        }
    }

    @Test
    fun unknownFundingFails() {
        val funding = listOf(utxo(AddressType.P2PKH, 0))
        assertFalse(verifiesAll(signedSpend(funding), emptyList()))
    }

    @Test
    fun anotherKeysCoinFails() {
        val funding = listOf(utxo(AddressType.P2WPKH, 0))
        val tx = signedSpend(funding)
        val otherScript = funding.map {
            UnspentTransactionOutput(it.outPoint, it.height, it.value,
                StandardTransactionBuilder.createOutput(stranger, it.value, network).script)
        }
        assertFalse(verifiesAll(tx, otherScript))
    }
}
