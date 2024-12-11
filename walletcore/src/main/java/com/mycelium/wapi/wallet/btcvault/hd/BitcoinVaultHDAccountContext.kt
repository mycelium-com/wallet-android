package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.AccountIndexesContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.util.*

/**
 * The context of an account
 */
class BitcoinVaultHDAccountContext @JvmOverloads constructor(
        val id: UUID,
        currency: CryptoCurrency,
        val accountIndex: Int,
        isArchived: Boolean,
        accountName: String,
        balance: Balance,
        val listener: (BitcoinVaultHDAccountContext) -> Unit,
        blockHeight: Int = 0,
        private var lastDiscovery: Long = 0,
        val indexesMap: Map<BipDerivationType, AccountIndexesContext> = createNewIndexesContexts(BipDerivationType.values().asIterable()),
        val accountType: Int = ACCOUNT_TYPE_FROM_MASTERSEED,
        val accountSubId: Int = 0,
        var defaultAddressType: AddressType = AddressType.P2SH_P2WPKH
) : AccountContextImpl(id, currency, accountName, balance, isArchived, blockHeight) {
    private var isDirty: Boolean = false

    override var archived: Boolean = isArchived
        get() = super.archived
        set(value) {
            if (field != value) {
                isDirty = true
                field = value
                onChange()
            }
        }

    fun getLastExternalIndexWithActivity(type: BipDerivationType): Int =
            indexesMap[type]?.lastExternalIndexWithActivity ?: -1

    internal fun setLastExternalIndexWithActivity(type: BipDerivationType, lastExternalIndexWithActivity: Int) {
        if (indexesMap[type]!!.lastExternalIndexWithActivity != lastExternalIndexWithActivity) {
            isDirty = true
            indexesMap[type]!!.lastExternalIndexWithActivity = lastExternalIndexWithActivity
        }
    }

    fun getLastInternalIndexWithActivity(type: BipDerivationType): Int =
            indexesMap[type]?.lastInternalIndexWithActivity ?: -1

    internal fun setLastInternalIndexWithActivity(type: BipDerivationType, lastInternalIndexWithActivity: Int) {
        if (indexesMap[type]!!.lastInternalIndexWithActivity != lastInternalIndexWithActivity) {
            isDirty = true
            indexesMap[type]!!.lastInternalIndexWithActivity = lastInternalIndexWithActivity
        }
    }

    fun getFirstMonitoredInternalIndex(type: BipDerivationType): Int {
        return indexesMap[type]!!.firstMonitoredInternalIndex
    }

    internal fun setFirstMonitoredInternalIndex(type: BipDerivationType, firstMonitoredInternalIndex: Int) {
        if (indexesMap[type]!!.firstMonitoredInternalIndex != firstMonitoredInternalIndex) {
            isDirty = true
            indexesMap[type]!!.firstMonitoredInternalIndex = firstMonitoredInternalIndex
        }
    }

    fun getLastDiscovery(): Long {
        return lastDiscovery
    }

    internal fun setLastDiscovery(lastDiscovery: Long) {
        if (this.lastDiscovery != lastDiscovery) {
            isDirty = true
            this.lastDiscovery = lastDiscovery
        }
    }

    /**
     * Persist this context if it is marked as dirty
     */
    fun persistIfNecessary(backing: BitcoinVaultHDAccountBacking) {
        if (isDirty) {
            persist(backing)
        }
    }

    /**
     * Persist this context
     */
    fun persist(backing: BitcoinVaultHDAccountBacking) {
        backing.updateAccountContext(this)
        isDirty = false
    }

    companion object {
        const val ACCOUNT_TYPE_FROM_MASTERSEED = 0
        const val ACCOUNT_TYPE_UNRELATED_X_PRIV = 1
        const val ACCOUNT_TYPE_UNRELATED_X_PUB = 2

        private fun createNewIndexesContexts(derivationTypes: Iterable<BipDerivationType>) =
                derivationTypes.map { it to AccountIndexesContext(-1, -1, 0) }
                        .toMap()
                        .toMutableMap()
    }

    override fun onChange() {
        listener(this)
    }
}
