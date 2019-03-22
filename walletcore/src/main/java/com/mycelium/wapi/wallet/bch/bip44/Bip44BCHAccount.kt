package com.mycelium.wapi.wallet.bch.bip44

import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SpvBalanceFetcher
import com.mycelium.wapi.wallet.bch.coins.BchMain
import com.mycelium.wapi.wallet.bch.coins.BchTest
import com.mycelium.wapi.wallet.btc.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_FROM_MASTERSEED
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance

import java.util.UUID

open class Bip44BCHAccount(
        context: HDAccountContext,
        keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters, backing: Bip44AccountBacking, wapi: Wapi) :
        HDAccount(context, keyManagerMap, network, backing, wapi, Reference(ChangeAddressMode.NONE)) {


    override fun getCurrencyBasedBalance(): CurrencyBasedBalance {
        return CurrencyBasedBalance.ZERO_BITCOIN_BALANCE
    }

    override fun getCoinType(): CryptoCurrency {
        return if (network.isProdnet) BchMain else BchTest
    }

    override fun getTransactionSummary(txid: Sha256Hash): TransactionSummary? {
        return null
    }

    override fun calculateMaxSpendableAmount(minerFeePerKbToUse: Long, destinationAddress: BtcAddress): Value {
        return Value.zeroValue(coinType)
    }

    override fun getId(): UUID {
        return UUID.randomUUID()
    }

    override fun setBlockChainHeight(blockHeight: Int) {}

    override fun getBlockChainHeight(): Int {
        return 0
    }

    override fun dropCachedData() {
        //BCH account have no separate context, so no cashed data, nothing to drop here
    }

    override fun getTransactionHistory(offset: Int, limit: Int): List<TransactionSummary> {
        return ArrayList()
    }

    private fun getBchTransaction(ts :  TransactionSummary): BchTransaction?{
        return null
    }

    override fun getTransactionsSince(receivingSince: Long): MutableList<BtcTransaction> {
        return ArrayList()
    }

    override fun isVisible(): Boolean {
        return false
    }

    private fun checkVisibility(): Boolean {
        return false
    }

    override fun getPrivateKeyCount(): Int {
        return 0
    }

    override fun getReceivingAddress(): Optional<Address> {
        return Optional.absent()
    }

    override fun getPrivateKeyForAddress(address: Address, cipher: KeyCipher): InMemoryPrivateKey? {
        return null
    }
}
