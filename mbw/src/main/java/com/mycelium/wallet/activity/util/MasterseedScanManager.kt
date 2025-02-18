package com.mycelium.wallet.activity.util

import android.content.Context
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.*
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.fio.FIOUnrelatedHDConfig
import com.squareup.otto.Bus
import java.util.UUID

class MasterseedScanManager : AbstractAccountScanManager {
    private var masterSeed: Bip39.MasterSeed? = null
    private val words: Array<String>?
    private var password: String?
    private var coinType: CryptoCurrency

    constructor(context: Context, network: NetworkParameters, masterSeed: Bip39.MasterSeed, eventBus: Bus,
                coinType: CryptoCurrency) : super(context, network, eventBus, coinType) {
        this.masterSeed = masterSeed
        this.words = null
        this.password = null
        this.coinType = coinType
    }

    constructor(context: Context, network: NetworkParameters, words: Array<String>, password: String?, eventBus: Bus,
                coinType: CryptoCurrency) : super(context, network, eventBus, coinType) {
        this.words = words.clone()
        this.password = password
        this.coinType = coinType
    }

    override fun onBeforeScan(): Boolean {
        if (masterSeed == null) {
            // use the provided passphrase, if it is nor null, ...
            if (password == null) {
                // .. otherwise query the user for a passphrase
                password = waitForPassphrase()
            }
            if (password != null) {
                masterSeed = Bip39.generateSeedFromWordList(words, password)
            } else {
                return false
            }
        }
        return true
    }

    override fun getAccountPubKeyNode(
        keyPath: HdKeyPath,
        derivationType: BipDerivationType
    ): HdKeyNode {
        // Generate the root private key
        //todo: caching of intermediary path sections
        val root = HdKeyNode.fromSeed(masterSeed!!.bip32Seed, derivationType)
        return root.createChildNode(keyPath)
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
            if (coinType == Utils.getBtcCoinType()) {
                walletManager.createAccounts(UnrelatedHDAccountConfig(accountRoots))[0]
            } else {
                walletManager.createAccounts(FIOUnrelatedHDConfig(accountRoots))[0]
            }
        }
    }

    override fun getAccountPathsToScan(
        lastPath: HdKeyPath?,
        wasUsed: Boolean,
        coinType: CryptoCurrency?
    ): Map<BipDerivationType, HdKeyPath> {
        // this is the first call - no lastPath given
        if (lastPath == null) {
            return mapOf(BipDerivationType.BIP44 to HdKeyPath.BIP32_ROOT,
                    BipDerivationType.BIP49 to HdKeyPath.BIP32_ROOT,
                    BipDerivationType.BIP84 to HdKeyPath.BIP32_ROOT)
        }

        // if the lastPath was the Bip32, we dont care if it wasUsed - always scan the first accounts
        return if (lastPath == HdKeyPath.BIP32_ROOT) {
            if (coinType == Utils.getBtcCoinType()) {
                mapOf(BipDerivationType.BIP44 to BIP44COIN_TYPE.getAccount(0),
                        BipDerivationType.BIP49 to BIP49COIN_TYPE.getAccount(0),
                        BipDerivationType.BIP84 to BIP84COIN_TYPE.getAccount(0))
            } else {
                mapOf(BipDerivationType.BIP44 to BIP44FIOCOIN_TYPE.getAccount(0))
            }
        } else {
            // otherwise just return the normal bip44 accounts
            super.getAccountPathsToScan(lastPath, wasUsed, coinType)
        }
    }
}
