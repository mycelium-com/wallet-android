/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.bitlib.model

import java.security.SecureRandom

import com.mrd.bitlib.util.HexUtils
import org.junit.Assert
import org.junit.Test

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.RandomSource

class AddressTest {

    @Test
    fun toStringTest() {
        val priv = InMemoryPrivateKey(RANDOM_SOURCE)
        val pub = priv.publicKey
        val addressList = pub.getAllSupportedAddresses(NetworkParameters.productionNetwork)
        println(addressList.map { it?.toString() }.toString())
    }

    @Test
    fun standardAddressTestFromBytes() {
        val address = Address.fromStandardBytes(
                HexUtils.toBytes("B169F2B0B866DB05900B93A5D76345F18D3AFB24"), NetworkParameters.productionNetwork)
        Assert.assertEquals("1HB5XMLmzFVj8ALj6mfBsbifRoD4miY36v", address!!.toString())
    }

    @Test
    fun standardAddressTest() {
        val tAddr = Address.fromString("muvtKjWtqcxrsDYfvFCgGnkmB4EqEcU8Bk")
        Assert.assertNotNull(tAddr)
        Assert.assertFalse(tAddr.isP2SH(tAddr.network))
        Assert.assertTrue(tAddr.network.isTestnet)

        val pAddr = Address.fromString("1NiKrdcsiat3NVRu5XCmGkzZhZDTGXabU5")
        Assert.assertNotNull(pAddr)
        Assert.assertFalse(pAddr.isP2SH(pAddr.network))
        Assert.assertTrue(pAddr.network.isProdnet)
    }

    @Test
    fun multisigAddressTest() {
        val tAddr = Address.fromString("2N9ZkpDh83uygvhTSy5syYADZvDuVZi8mRH")
        Assert.assertNotNull(tAddr)
        Assert.assertTrue(tAddr.isP2SH(tAddr.network))
        Assert.assertTrue(tAddr.network.isTestnet)

        val pAddr = Address.fromString("31qh3GkM3RLPfMy86XjisS7bVkz7Pz8wee")
        Assert.assertNotNull(pAddr)
        Assert.assertTrue(pAddr.isP2SH(pAddr.network))
        Assert.assertTrue(pAddr.network.isProdnet)
    }

    companion object {
        private val RANDOM_SOURCE = RandomSource { bytes -> SecureRandom().nextBytes(bytes) }
    }

}
