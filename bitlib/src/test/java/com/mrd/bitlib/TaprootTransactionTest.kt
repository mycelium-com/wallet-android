package com.mrd.bitlib

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.SegwitAddress
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.TaprootUtils
import org.junit.Assert
import org.junit.Test

//https://github.com/bitcoin/bips/blob/master/bip-0086.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0340.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0341.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0386.mediawiki

class TaprootTransactionTest {

    //mnemonic = abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
    // Account 0, first receiving address = m/84'/0'/0'/0/0
    val bip84PrivateKey = InMemoryPrivateKey("KyZpNDKnfs94vbrwhJneDi77V6jF64PWPF8x5cdJb8ifgg2DUc9d",
            NetworkParameters.productionNetwork)
    val bip84PublicKey = PublicKey(HexUtils.toBytes("0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"))
    val bip84Address = BitcoinAddress.fromString("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu")

    // Account 0, first receiving address = m/86'/0'/0'/0/0
    val bip86PrivateKey = InMemoryPrivateKey("KyRv5iFPHG7iB5E4CqvMzH3WFJVhbfYK4VY7XAedd9Ys69mEsPLQ",
            NetworkParameters.productionNetwork)
    val bip86PublicKey = PublicKey(HexUtils.toBytes("03cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"))
    val bip86InternalKey = HexUtils.toBytes("cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115")
    val bip86OutputKey = HexUtils.toBytes("a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c")
    val bip86Address = BitcoinAddress.fromString("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr")

    @Test
    fun testBip84Keys() {
        Assert.assertEquals(bip84PrivateKey.publicKey.toAddress(NetworkParameters.productionNetwork, AddressType.P2WPKH), bip84Address)
        Assert.assertArrayEquals(bip84PrivateKey.publicKey.publicKeyBytes, bip84PublicKey.publicKeyBytes)
    }

    @Test
    fun testTaggedHash() {
        val hash = TaprootUtils.taggedHash("SampleTagName", "Input data".toByteArray())
        Assert.assertEquals("4c55df56134d7f37d3295850659f2e3729128c969b3386ec661feb7dfe29a99c", HexUtils.toHex(hash))
    }

    @Test
    fun testAddressFromOutpurKey() {
        val address = SegwitAddress(NetworkParameters.productionNetwork, 1, bip86OutputKey)
        Assert.assertEquals(bip86Address, address)
    }

    @Test
    fun testOutputKeyFromInternalKey() {
        val outputKey = TaprootUtils.outputKey(Parameters.curve.decodePoint(HexUtils.toBytes("02") + bip86InternalKey))
        Assert.assertArrayEquals(bip86OutputKey, outputKey)
    }

    @Test
    fun testBip86Keys() {
        Assert.assertEquals(bip86PrivateKey.publicKey.Q.x.toBigInteger(), bip86PublicKey.Q.x.toBigInteger())

        val internalKey = TaprootUtils.lift_x(bip86PublicKey.Q)
        val k = internalKey!!.x.toBigInteger().toByteArray().copyOfRange(1, internalKey.x.toBigInteger().toByteArray().size)
        Assert.assertArrayEquals(bip86InternalKey, k)

        val outputKey = TaprootUtils.outputKey(internalKey)
        Assert.assertArrayEquals(bip86OutputKey, outputKey)

        val trAddress = SegwitAddress(NetworkParameters.productionNetwork, 1, outputKey)
        Assert.assertEquals(bip86Address, trAddress)
    }

    @Test
    fun testPublicKey() {
        Assert.assertEquals(bip86Address, bip86PublicKey.toAddress(NetworkParameters.productionNetwork, AddressType.P2TR))
    }

}

