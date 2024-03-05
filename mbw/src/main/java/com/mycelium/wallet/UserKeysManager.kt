/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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
package com.mycelium.wallet

import com.mycelium.wapi.wallet.AesKeyCipher
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.ECKeyPairGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECKeyGenerationParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.prng.FixedSecureRandom
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security


object UserKeysManager {
    private val mbwManger = MbwManager.getInstance(WalletApplication.getInstance())
    val userSignKeys = getDeterministicECKeyPair()
    private fun getDeterministicECKeyPair(): AsymmetricCipherKeyPair {
        val masterSeed = mbwManger.masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())

        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        // generate determine asymmetric keys using master seed as random seed
        val hkdfGenerator = HKDFBytesGenerator(SHA256Digest())
        hkdfGenerator.init(HKDFParameters(masterSeed.bip32Seed, null, ByteArray(0)))
        val privateKeyBytes = ByteArray(32)
        hkdfGenerator.generateBytes(privateKeyBytes, 0, privateKeyBytes.size)

        val ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val domainParameters = ECDomainParameters(ecSpec.curve, ecSpec.g, ecSpec.n, ecSpec.h)
        val random = FixedSecureRandom(privateKeyBytes)

        val keyGenParams = ECKeyGenerationParameters(domainParameters, random)
        val keyPairGenerator = ECKeyPairGenerator()
        keyPairGenerator.init(keyGenParams)
        return keyPairGenerator.generateKeyPair()
    }

}