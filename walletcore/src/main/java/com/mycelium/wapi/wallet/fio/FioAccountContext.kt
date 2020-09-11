package com.mycelium.wapi.wallet.fio

import com.mycelium.generated.wallet.database.FioContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.math.BigInteger
import java.util.*

class FioAccountContext(override val uuid: UUID,
                        currency: CryptoCurrency,
                        accountName: String,
                        balance: Balance,
                        listener: (FioAccountContext) -> Unit,
                        override val accountIndex: Int,
                        registeredFIONames: List<String>? = null,
                        archived: Boolean = false,
                        blockHeight: Int = 0,
                        actionSequenceNumber: BigInteger = BigInteger.ZERO) :
        FioContext by FioContext.Impl(uuid, accountIndex, actionSequenceNumber, registeredFIONames),
        AccountContextImpl<FioAccountContext>(uuid, currency, accountName, balance, listener, archived, blockHeight) {
    override var actionSequenceNumber = actionSequenceNumber
        set(value) {
            field = value
            listener.invoke(this)
        }
    override var registeredFIONames = registeredFIONames
        set(value) {
            field = value
            listener.invoke(this)
        }
}