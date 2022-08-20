package com.mrd.bitlib

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PrivateKey
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

    class Data(val path: String,
               val privateKey: PrivateKey,
               val publicKey: PublicKey,
               val internalKey: ByteArray,
               val outputKey: ByteArray,
               val address: BitcoinAddress)

    val testData = listOf(
            // Account 0, first receiving address = m/86'/0'/0'/0/0
            Data("m/86'/0'/0'/0/0",
                    InMemoryPrivateKey("KyRv5iFPHG7iB5E4CqvMzH3WFJVhbfYK4VY7XAedd9Ys69mEsPLQ", NetworkParameters.productionNetwork),
                    PublicKey(HexUtils.toBytes("03cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115")),
                    HexUtils.toBytes("cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"),
                    HexUtils.toBytes("a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c"),
                    BitcoinAddress.fromString("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr")),

            // Account 0, second receiving address = m/86'/0'/0'/0/1
            Data("m/86'/0'/0'/0/1",
                    InMemoryPrivateKey("L1jhNnZZAAAppoSYQuaAQEj935VpmishMomuWXgJ3Qy5HNqkhhus", NetworkParameters.productionNetwork),
                    PublicKey(HexUtils.toBytes("0283dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145")),
                    HexUtils.toBytes("83dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145"),
                    HexUtils.toBytes("a82f29944d65b86ae6b5e5cc75e294ead6c59391a1edc5e016e3498c67fc7bbb"),
                    BitcoinAddress.fromString("bc1p4qhjn9zdvkux4e44uhx8tc55attvtyu358kutcqkudyccelu0was9fqzwh")),

            // Account 0, first change address = m/86'/0'/0'/1/0
            Data("m/86'/0'/0'/1/0",
                    InMemoryPrivateKey("KzsCLFtWKpeNKMHFyHKT8vGRuGQxEY8CQjgLcEj14C8xK2PyEFeN", NetworkParameters.productionNetwork),
                    PublicKey(HexUtils.toBytes("02399f1b2f4393f29a18c937859c5dd8a77350103157eb880f02e8c08214277cef")),
                    HexUtils.toBytes("399f1b2f4393f29a18c937859c5dd8a77350103157eb880f02e8c08214277cef"),
                    HexUtils.toBytes("882d74e5d0572d5a816cef0041a96b6c1de832f6f9676d9605c44d5e9a97d3dc"),
                    BitcoinAddress.fromString("bc1p3qkhfews2uk44qtvauqyr2ttdsw7svhkl9nkm9s9c3x4ax5h60wqwruhk7"))
    )

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
        testData.forEach {
            val address = SegwitAddress(NetworkParameters.productionNetwork, 1, it.outputKey)
            Assert.assertEquals(it.address, address)
        }
    }

    @Test
    fun testOutputKeyFromInternalKey() {
        testData.forEach {
            val outputKey = TaprootUtils.outputKey(Parameters.curve.decodePoint(HexUtils.toBytes("02") + it.internalKey))
            Assert.assertArrayEquals(it.outputKey, outputKey)
        }
    }

    @Test
    fun testBip86Keys() {
        testData.forEach {
            Assert.assertEquals(it.privateKey.publicKey.Q.x.toBigInteger(), it.publicKey.Q.x.toBigInteger())

            val internalKey = TaprootUtils.lift_x(it.publicKey.Q)
            val k = internalKey!!.x.toBigInteger().toByteArray().let {
                if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
            }
//            Logger.getLogger("!!!!").log(Level.SEVERE, "internalKey" + HexUtils.toHex(k))
            Assert.assertArrayEquals(it.internalKey, k)

            val outputKey = TaprootUtils.outputKey(internalKey)
            Assert.assertArrayEquals(it.outputKey, outputKey)

            val trAddress = SegwitAddress(NetworkParameters.productionNetwork, 1, outputKey)
            Assert.assertEquals(it.address, trAddress)
        }
    }

    @Test
    fun testPublicKey() {
        testData.forEach {
            Assert.assertEquals(it.address, it.publicKey.toAddress(NetworkParameters.productionNetwork, AddressType.P2TR))
        }
    }

}

