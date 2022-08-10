package com.mrd.bitlib

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test


// https://github.com/bitcoin-core/btcdeb/blob/master/doc/tapscript-example-with-tap.md


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
    val bip86Address = BitcoinAddress.fromString("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr")

    @Test
    fun testBip84Keys() {
        Assert.assertEquals(bip84PrivateKey.publicKey.toAddress(NetworkParameters.productionNetwork, AddressType.P2WPKH), bip84Address)
        Assert.assertArrayEquals(bip84PrivateKey.publicKey.publicKeyBytes, bip84PublicKey.publicKeyBytes)
    }

    @Test
    fun testBip86Keys() {
        Assert.assertArrayEquals(bip86PrivateKey.publicKey.publicKeyBytes, bip86PublicKey.publicKeyBytes)
        Assert.assertEquals(bip86Address,
                bip86PrivateKey.publicKey.toAddress(NetworkParameters.productionNetwork, AddressType.P2TR, true))
    }
}

