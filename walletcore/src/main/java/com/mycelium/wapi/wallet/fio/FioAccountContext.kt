package com.mycelium.wapi.wallet.fio

import com.mycelium.generated.wallet.database.FioContext
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.math.BigInteger
import java.util.*

class FioAccountContext(override val uuid: UUID,
                        currency: CryptoCurrency,
                        accountName: String,
                        balance: Balance,
                        val listener: (FioAccountContext) -> Unit,
                        override val accountIndex: Int,
                        registeredFIONames: List<RegisteredFIOName>? = null,
                        registeredFIODomains: List<FIODomain>? = null,
                        archived: Boolean = false,
                        blockHeight: Int = 0,
                        accountType: Int = ACCOUNT_TYPE_FROM_MASTERSEED,
                        actionSequenceNumber: BigInteger = BigInteger.ZERO) :
        FioContext by FioContext.Impl(uuid, accountIndex, accountType, actionSequenceNumber, registeredFIONames, registeredFIODomains),
        AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {
    override fun onChange() {
        listener(this)
    }

    override var actionSequenceNumber = actionSequenceNumber
        set(value) {
            field = value
            onChange()
        }

    override var registeredFIONames = registeredFIONames
        set(value) {
            field = value
            onChange()
        }

    override var registeredFIODomains = registeredFIODomains
        set(value) {
            field = value
            onChange()
        }

    companion object {
        const val ACCOUNT_TYPE_FROM_MASTERSEED = 0
        const val ACCOUNT_TYPE_UNRELATED_X_PRIV = 1
        const val ACCOUNT_TYPE_UNRELATED_X_PUB = 2
    }
}