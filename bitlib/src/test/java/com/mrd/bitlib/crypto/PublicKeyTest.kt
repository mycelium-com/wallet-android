package com.mrd.bitlib.crypto

import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils.toBytes
import org.junit.Test

import org.junit.Assert.*

class PublicKeyTest {
    // we found problems spending from a SA account derived from 91tVaPm7poHgaNx2vYKtim84ykVzoB8opPv1dgieHvoAAHSmyX1
    // 91tVaPm7poHgaNx2vYKtim84ykVzoB8opPv1dgieHvoAAHSmyX1 is an uncompressed testnet Private Key WIF 51 characters base58
    val sk = InMemoryPrivateKey("91tVaPm7poHgaNx2vYKtim84ykVzoB8opPv1dgieHvoAAHSmyX1", NetworkParameters.testNetwork)
    val pk = sk.publicKey

    @Test
    fun getQ() {
        val pk = PublicKey(toBytes("03ad1d8e89212f0b92c74d23bb710c00662ad1470198ac48c43f7d6f93a2a26873"))
        assertTrue(pk.Q.isCompressed)
    }

    @Test
    fun uncompressedYieldsP2PKHandP2SH_P2WPKH() {
        assertFalse(pk.Q.isCompressed)
        val allAddresses = pk.getAllSupportedAddresses(NetworkParameters.testNetwork)
        assertNotNull("We should get a p2pkh address ... ", allAddresses[AddressType.P2PKH])
        assertNotNull("We should get a p2sh_p2wpkh address ... ", allAddresses[AddressType.P2SH_P2WPKH])
        assertEquals("and only those two addresses.", 2, allAddresses.size)
    }

    @Test(expected = IllegalStateException::class)
    fun P2WPKHFromUncompressedYieldsNull() {
        assertNull(pk.toAddress(NetworkParameters.testNetwork, AddressType.P2WPKH))
    }
}
