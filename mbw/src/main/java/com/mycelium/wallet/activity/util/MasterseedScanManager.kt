/*
 * Copyright 2013 - 2018 Megion Research and Development GmbH
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

package com.mycelium.wallet.activity.util

import android.content.Context
import com.google.common.base.Optional
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.*
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.WalletManager
import com.squareup.otto.Bus
import java.util.UUID

class MasterseedScanManager : AbstractAccountScanManager {
    private var masterSeed: Bip39.MasterSeed? = null
    private val words: Array<String>?
    private val password: String?

    constructor(context: Context, network: NetworkParameters, masterSeed: Bip39.MasterSeed, eventBus: Bus) : super(context, network, eventBus) {
        this.masterSeed = masterSeed
        this.words = null
        this.password = null
    }

    constructor(context: Context, network: NetworkParameters, words: Array<String>, password: String?, eventBus: Bus) : super(context, network, eventBus) {
        this.words = words.clone()
        this.password = password
    }

    override fun onBeforeScan(): Boolean {
        if (masterSeed == null) {
            // use the provided passphrase, if it is nor null, ...
            var passphrase = Optional.fromNullable(password)
            if (!passphrase.isPresent) {
                // .. otherwise query the user for a passphrase
                passphrase = waitForPassphrase()
            }
            if (passphrase.isPresent) {
                masterSeed = Bip39.generateSeedFromWordList(words, passphrase.get())
            } else {
                return false
            }
        }
        return true
    }

    override fun getAccountPubKeyNode(keyPath: HdKeyPath, derivationType: BipDerivationType): Optional<HdKeyNode>? {
        // Generate the root private key
        //todo: caching of intermediary path sections
        val root = HdKeyNode.fromSeed(masterSeed!!.bip32Seed, derivationType)
        return Optional.of(root.createChildNode(keyPath))
    }

    override fun upgradeAccount(accountRoots: List<HdKeyNode>, walletManager: WalletManager, uuid: UUID): Boolean {
        // This is not needed for in-wallet accounts
        return false
    }

    override fun createOnTheFlyAccount(accountRoots: List<HdKeyNode>, walletManager: WalletManager, accountIndex: Int): UUID {
        val uuids = accountRoots.filter { walletManager.hasAccount(it.uuid) }
        return if (uuids.isNotEmpty()) {
            // Account already exists
            uuids[0].uuid
        } else {
            walletManager.createUnrelatedBip44Account(accountRoots)
        }
    }

    override fun getAccountPathsToScan(lastPath: HdKeyPath?, wasUsed: Boolean): Map<BipDerivationType, HdKeyPath> {
        // this is the first call - no lastPath given
        if (lastPath == null) {
            return mapOf(BipDerivationType.BIP44 to HdKeyPath.BIP32_ROOT,
                    BipDerivationType.BIP49 to HdKeyPath.BIP32_ROOT,
                    BipDerivationType.BIP84 to HdKeyPath.BIP32_ROOT)
        }

        // if the lastPath was the Bip32, we dont care if it wasUsed - always scan the first accounts
        return if (lastPath == HdKeyPath.BIP32_ROOT) {
            mapOf(BipDerivationType.BIP44 to BIP44COIN_TYPE.getAccount(0),
                    BipDerivationType.BIP49 to BIP49COIN_TYPE.getAccount(0),
                    BipDerivationType.BIP84 to BIP84COIN_TYPE.getAccount(0))
        } else {
            // otherwise just return the normal bip44 accounts
            super.getAccountPathsToScan(lastPath, wasUsed)
        }
    }
}

