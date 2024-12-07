package com.mrd.bitlib

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.ec.Parameters
import com.mrd.bitlib.crypto.schnorr.SchnorrSign
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.SegwitAddress
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mrd.bitlib.util.TaprootUtils
import com.mrd.bitlib.util.cutStartByteArray
import com.mrd.bitlib.util.toByteArray
import org.junit.Assert
import org.junit.Test

//https://github.com/bitcoin/bips/blob/master/bip-0086.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0340.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0341.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki
//https://github.com/bitcoin/bips/blob/master/bip-0386.mediawiki
//https://bitcoiner.guide/seed/ taproot address generator

class TaprootTransactionTest {

    val auxRand =
        HexUtils.toBytes("0000000000000000000000000000000000000000000000000000000000000000")


    //mnemonic = abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about
    // Account 0, first receiving address = m/84'/0'/0'/0/0
    val bip84PrivateKey = InMemoryPrivateKey(
        "KyZpNDKnfs94vbrwhJneDi77V6jF64PWPF8x5cdJb8ifgg2DUc9d",
        NetworkParameters.productionNetwork
    )
    val bip84PublicKey =
        PublicKey(HexUtils.toBytes("0330d54fd0dd420a6e5f8d3624f5f3482cae350f79d5f0753bf5beef9c2d91af3c"))
    val bip84Address = BitcoinAddress.fromString("bc1qcr8te4kr609gcawutmrza0j4xv80jy8z306fyu")

    class Data(
        val path: String,
        val privateKey: PrivateKey,
        val publicKey: PublicKey,
        val internalKey: ByteArray,
        val outputKey: ByteArray,
        val address: BitcoinAddress
    )

    val testData = listOf(
        // Account 0, first receiving address = m/86'/0'/0'/0/0
        Data(
            "m/86'/0'/0'/0/0",
            InMemoryPrivateKey(
                "KyRv5iFPHG7iB5E4CqvMzH3WFJVhbfYK4VY7XAedd9Ys69mEsPLQ",
                NetworkParameters.productionNetwork
            ),
            PublicKey(HexUtils.toBytes("03cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115")),
            HexUtils.toBytes("cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"),
            HexUtils.toBytes("a60869f0dbcf1dc659c9cecbaf8050135ea9e8cdc487053f1dc6880949dc684c"),
            BitcoinAddress.fromString("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr")
        ),

        // Account 0, second receiving address = m/86'/0'/0'/0/1
        Data(
            "m/86'/0'/0'/0/1",
            InMemoryPrivateKey(
                "L1jhNnZZAAAppoSYQuaAQEj935VpmishMomuWXgJ3Qy5HNqkhhus",
                NetworkParameters.productionNetwork
            ),
            PublicKey(HexUtils.toBytes("0283dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145")),
            HexUtils.toBytes("83dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145"),
            HexUtils.toBytes("a82f29944d65b86ae6b5e5cc75e294ead6c59391a1edc5e016e3498c67fc7bbb"),
            BitcoinAddress.fromString("bc1p4qhjn9zdvkux4e44uhx8tc55attvtyu358kutcqkudyccelu0was9fqzwh")
        ),

        // Account 0, first change address = m/86'/0'/0'/1/0
        Data(
            "m/86'/0'/0'/1/0",
            InMemoryPrivateKey(
                "KzsCLFtWKpeNKMHFyHKT8vGRuGQxEY8CQjgLcEj14C8xK2PyEFeN",
                NetworkParameters.productionNetwork
            ),
            PublicKey(HexUtils.toBytes("02399f1b2f4393f29a18c937859c5dd8a77350103157eb880f02e8c08214277cef")),
            HexUtils.toBytes("399f1b2f4393f29a18c937859c5dd8a77350103157eb880f02e8c08214277cef"),
            HexUtils.toBytes("882d74e5d0572d5a816cef0041a96b6c1de832f6f9676d9605c44d5e9a97d3dc"),
            BitcoinAddress.fromString("bc1p3qkhfews2uk44qtvauqyr2ttdsw7svhkl9nkm9s9c3x4ax5h60wqwruhk7")
        )
    )

    @Test
    fun testBip84Keys() {
        Assert.assertEquals(
            bip84PrivateKey.publicKey.toAddress(
                NetworkParameters.productionNetwork,
                AddressType.P2WPKH
            ), bip84Address
        )
        Assert.assertArrayEquals(
            bip84PrivateKey.publicKey.publicKeyBytes,
            bip84PublicKey.publicKeyBytes
        )
    }

    @Test
    fun testTaggedHash() {
        val hash = TaprootUtils.taggedHash("SampleTagName", "Input data".toByteArray())
        Assert.assertEquals(
            "4c55df56134d7f37d3295850659f2e3729128c969b3386ec661feb7dfe29a99c",
            hash.toHex()
        )
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
            val outputKey =
                TaprootUtils.outputKey(Parameters.curve.decodePoint(HexUtils.toBytes("02") + it.internalKey))
            Assert.assertArrayEquals(it.outputKey, outputKey)
        }
    }

    @Test
    fun testBip86Keys() {
        testData.forEach {
            Assert.assertEquals(
                it.privateKey.publicKey.Q.x.toBigInteger(),
                it.publicKey.Q.x.toBigInteger()
            )

            val internalKey = TaprootUtils.liftX(it.publicKey.Q)
            val k = internalKey.x.toByteArray(32).let {
                if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
            }
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
            Assert.assertEquals(
                it.address,
                it.publicKey.toAddress(NetworkParameters.productionNetwork, AddressType.P2TR)
            )
        }
    }

    @Test
    fun testPrivateKey() {
        testData.forEach {
            Assert.assertEquals(
                it.address,
                it.privateKey.publicKey.toAddress(
                    NetworkParameters.productionNetwork,
                    AddressType.P2TR
                )
            )
        }
    }

    @Test
    fun testTapTweak() {
        val testData =
            HexUtils.toBytes("6afea324cbe1d2d8ee051267e9fb869941a53b59e8ba27978e181d3344f46dbc")
        val testResult = "0e6bd305b819c2d779bbd91eb965f02ec639c3f2ae488270e3ce868df1fb9aec"
        val tapTweak = TaprootUtils.hashTapTweak(testData)
        Assert.assertEquals(tapTweak.toHex(), testResult)
    }

    val PRIVATE_KEY =
        HexUtils.toBytes("55d7c5a9ce3d2b15a62434d01205f3e59077d51316f5c20628b3a4b8b2a76f4c")
    val PUBLIC_KEY =
        HexUtils.toBytes("924c163b385af7093440184af6fd6244936d1288cbb41cc3812286d3f83a3329")

    @Test
    fun testTapTweakedPublic() {
        val publicKey =
            PublicKey(HexUtils.toBytes("03") + PUBLIC_KEY)
        val tPK = TaprootUtils.tweakedPublicKey(publicKey, ByteArray(0))
        Assert.assertEquals(
            HexUtils.toHex(tPK),
            "0f0c8db753acbd17343a39c2f3f4e35e4be6da749f9e35137ab220e7b238a667"
        )
    }

    @Test
    fun testTapRootTransaction() {
        val privateKey =
            InMemoryPrivateKey(HexUtils.toBytes("962d6e6a2d807b96fb17da9a0f1e7e03765f31aa26d324f3f0586b757a8670cf"))

        val pubKey =
            HexUtils.toBytes("21afb776c0926e185f606de872852d9fd120b7307e1402d3a433eb74f76a0b19")
        val publicKey = PublicKey(HexUtils.toBytes("03") + pubKey)


        println("tweak pub key = ${TaprootUtils.hashTapTweak(pubKey).toHex()}")

        Assert.assertEquals(
            privateKey.publicKey.toAddress(NetworkParameters.testNetwork, AddressType.P2TR),
            publicKey.toAddress(NetworkParameters.testNetwork, AddressType.P2TR)
        )

        Assert.assertEquals(
            HexUtils.toHex(pubKey),
            HexUtils.toHex(privateKey.publicKey.pubKeyCompressed.cutStartByteArray(32))
        )

        val address =
            privateKey.publicKey.toAddress(NetworkParameters.testNetwork, AddressType.P2TR)
        println("address = ${address}")
//        Assert.assertArrayEquals()
    }


    @Test
    fun testTweakPrivateKey() {
        val privateKey =
            HexUtils.toBytes("ce1fc7baa9db31c4ef9c6564f70d551f41fc479bb23fa844d50848220edaaf91")

        val tweak =
            HexUtils.toBytes("bf0094eae70ba67e2f9fc3c4b81f078c90931855a8d24c959619174c92060cde")

        Assert.assertEquals(
            "f0e0cd303d3074b940035e5fc111b26c0945ada0a5db448c80e32db753619e8e",
            HexUtils.toHex(TaprootUtils.tweakedPrivateKey(privateKey, Sha256Hash.of(tweak))),
        )
    }

    @Test
    fun testTx() {
        val privateKey = PRIVATE_KEY
        val publicKey = PUBLIC_KEY

        val sighash =
            HexUtils.toBytes("a7b390196945d71549a2454f0185ece1b47c56873cf41789d78926852c355132")

        val merkle = ByteArray(0)

        val pK = InMemoryPrivateKey(privateKey)
        Assert.assertEquals(
            HexUtils.toHex(publicKey),
            HexUtils.toHex(pK.publicKey.pubKeyCompressed.cutStartByteArray(32))
        )

        val tweak1 = TaprootUtils.tweak(publicKey, merkle)
        Assert.assertEquals(
            "8dc8b9030225e044083511759b58328b46dffcc78b920b4b97169f9d7b43d3b5",
            tweak1.toHex()
        )

        val tweakedPrivateKey = TaprootUtils.tweakedPrivateKey(privateKey, tweak1)
        val signature1 = SchnorrSign(tweakedPrivateKey).sign(sighash, auxRand)
        Assert.assertEquals(
            "b693a0797b24bae12ed0516a2f5ba765618dca89b75e498ba5b745b71644362298a45ca39230d10a02ee6290a91cebf9839600f7e35158a447ea182ea0e022ae",
            HexUtils.toHex(signature1)
        )


        val signature2 = pK.makeSchnorrBitcoinSignature(sighash, ByteArray(0), auxRand)
        Assert.assertEquals(
            "b693a0797b24bae12ed0516a2f5ba765618dca89b75e498ba5b745b71644362298a45ca39230d10a02ee6290a91cebf9839600f7e35158a447ea182ea0e022ae",
            HexUtils.toHex(signature2).substring(0, 128)
        )
    }
}