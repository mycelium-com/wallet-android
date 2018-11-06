package com.mycelium.wapi.wallet.bip44

import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.model.TransactionDetails
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_FROM_MASTERSEED
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue

import java.util.UUID

open class Bip44BCHAccount(
        context: HDAccountContext,
        keyManagerMap: MutableMap<BipDerivationType, HDAccountKeyManager>,
        network: NetworkParameters,
        backing: Bip44AccountBacking,
        wapi: Wapi,
        private val spvBalanceFetcher: SpvBalanceFetcher
) : HDAccount(context, keyManagerMap, network, backing, wapi, Reference(ChangeAddressMode.NONE)) {
    private var blockChainHeight = 0
    private var visible = false

    override fun getAccountDefaultCurrency(): String {
        return CurrencyValue.BCH
    }

    init {
        this.type = WalletAccount.Type.BCHBIP44
    }

    override fun getCurrencyBasedBalance(): CurrencyBasedBalance {
        return if (accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            spvBalanceFetcher.retrieveByHdAccountIndex(id.toString(), accountIndex)
        } else {
            spvBalanceFetcher.retrieveByUnrelatedAccountId(id.toString())
        }
    }

    override fun getTransactionDetails(txid: Sha256Hash): TransactionDetails {
        return spvBalanceFetcher.retrieveTransactionDetails(txid)
    }

    override fun getTransactionSummary(txid: Sha256Hash): TransactionSummary? {
        val transactions = spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(id.toString(),
                accountIndex)
        return transactions.firstOrNull { it.txid == txid }
    }

    override fun calculateMaxSpendableAmount(minerFeePerKbToUse: Long): ExactCurrencyValue {
        //TODO Refactor the code and make the proper usage of minerFeePerKbToUse parameter
        val txFee = "NORMAL"
        val txFeeFactor = 1.0f
        return if (accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            ExactBitcoinCashValue.from(spvBalanceFetcher.calculateMaxSpendableAmount(accountIndex, txFee, txFeeFactor))
        } else {
            ExactBitcoinCashValue.from(spvBalanceFetcher.calculateMaxSpendableAmountUnrelatedAccount(id.toString(), txFee, txFeeFactor))
        }
    }

    override fun calculateMaxSpendableAmount(minerFeePerKbToUse: Long, destinationAddress: Address?) =
        calculateMaxSpendableAmount(minerFeePerKbToUse)

    override fun getId(): UUID {
        return UUID.nameUUIDFromBytes(("BCH" + super.getId().toString()).toByteArray())
    }

    // need override because parent write it to context(bch and btc account have one context)
    override fun setBlockChainHeight(blockHeight: Int) {
        blockChainHeight = blockHeight
    }

    override fun getBlockChainHeight(): Int {
        return blockChainHeight
    }

    override fun dropCachedData() {
        //BCH account have no separate context, so no cashed data, nothing to drop here
    }

    override fun getTransactionHistory(offset: Int, limit: Int): List<TransactionSummary> {
        return if (accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(id.toString(), accountIndex, offset, limit)
                    .filter { it.height >= forkBlock }
        } else {
            spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString(), offset, limit)
                    .filter { it.height >= forkBlock }
        }
    }

    override fun getTransactionsSince(receivingSince: Long?): List<TransactionSummary> {
        return if (accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(id.toString(), accountIndex, receivingSince!!)
        } else {
            spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString(), receivingSince!!)
        }
    }

    override fun isVisible(): Boolean {
        if (!visible && (spvBalanceFetcher.syncProgressPercents == 100f || spvBalanceFetcher.isAccountSynced(this))) {
            visible = checkVisibility()
            if (visible) {
                spvBalanceFetcher.setVisible(this)
            }
        }
        return visible
    }

    private fun checkVisibility(): Boolean {
        return spvBalanceFetcher.isAccountVisible(this) || if (accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            !spvBalanceFetcher.retrieveTransactionsSummaryByHdAccountIndex(id.toString(), accountIndex).isEmpty()
        } else {
            !spvBalanceFetcher.retrieveTransactionsSummaryByUnrelatedAccountId(id.toString()).isEmpty()
        }
    }

    override fun getPrivateKeyCount(): Int {
        return if (accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            val info = spvBalanceFetcher.getPrivateKeysCount(accountIndex)
            info.externalKeys + info.internalKeys
        } else {
            val info = spvBalanceFetcher.getPrivateKeysCountUnrelated(id.toString())
            info.externalKeys + info.internalKeys
        }
    }

    override fun getReceivingAddress(): Optional<Address> {
        return Optional.fromNullable(spvBalanceFetcher.getCurrentReceiveAddress(accountIndex))
    }

    @Throws(KeyCipher.InvalidKeyCipher::class)
    override fun getPrivateKeyForAddress(address: Address, cipher: KeyCipher): InMemoryPrivateKey? {
        val info = spvBalanceFetcher.getPrivateKeysCount(accountIndex)
        val internalAddresses = getAddressRange(true, 0,
                info.internalKeys + HDAccount.INTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH, BipDerivationType.BIP44)
        val externalAddresses = getAddressRange(false, 0,
                info.externalKeys + HDAccount.EXTERNAL_FULL_ADDRESS_LOOK_AHEAD_LENGTH, BipDerivationType.BIP44)

        val iix = internalAddresses.indexOf(address)

        if (iix != -1) {
            return keyManagerMap[BipDerivationType.BIP44]!!.getPrivateKey(true, iix, cipher)
        }

        val eix = externalAddresses.indexOf(address)

        return if (eix != -1) {
            keyManagerMap[BipDerivationType.BIP44]!!.getPrivateKey(false, eix, cipher)
        } else null

    }

    companion object {
        private const val forkBlock = 478559
    }
}
