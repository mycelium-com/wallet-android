package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

abstract class AccountContextImpl(uuid: UUID,
                                  currency: CryptoCurrency,
                                  accountName: String,
                                  balance: Balance,
                                  archived: Boolean = false,
                                  blockHeight: Int = 0) :
        AccountContext by AccountContext.Impl(
                uuid,
                currency,
                accountName,
                balance,
                archived,
                blockHeight) {

    abstract fun onChange()

    override var archived = archived
        set(value) {
            field = value
            onChange()
        }

    override var accountName = accountName
        set(value) {
            field = value
            onChange()
        }

    override var balance = balance
        set(value) {
            field = value
            onChange()
        }

    override var blockHeight = blockHeight
        set(value) {
            field = value
            onChange()
        }
}

