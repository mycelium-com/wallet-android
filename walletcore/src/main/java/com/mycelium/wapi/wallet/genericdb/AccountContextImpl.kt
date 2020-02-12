package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

open class AccountContextImpl<Context : AccountContext>(uuid: UUID,
                                                        currency: CryptoCurrency,
                                                        accountName: String,
                                                        balance: Balance,
                                                        val listener: (Context) -> Unit,
                                                        archived: Boolean = false,
                                                        blockHeight: Int = 0) :
        AccountContext by AccountContext.Impl(
                uuid,
                currency,
                accountName,
                balance,
                archived,
                blockHeight) {
    override var archived = archived
        set(value) {
            field = value
            listener.invoke(this as Context)
        }

    override var accountName = accountName
        set(value) {
            field = value
            listener.invoke(this as Context)
        }

    override var balance = balance
        set(value) {
            field = value
            listener.invoke(this as Context)
        }

    override var blockHeight = blockHeight
        set(value) {
            field = value
            listener.invoke(this as Context)
        }
}

