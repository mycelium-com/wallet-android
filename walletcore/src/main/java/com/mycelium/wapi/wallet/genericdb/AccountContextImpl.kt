package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

class AccountContextImpl(uuid: UUID,
                         currency: CryptoCurrency,
                         accountName: String,
                         balance: Balance,
                         val listener: (AccountContext) -> Unit,
                         archived: Boolean = false) :
        AccountContext by AccountContext.Impl(
                uuid,
                currency,
                accountName,
                balance,
                archived) {
    override var archived = archived
        set(value) {
            field = value
            listener.invoke(this)
        }
    override var accountName = accountName
        set(value) {
            field = value
            listener.invoke(this)
        }
    override var balance = balance
        set(value) {
            field = value
            listener.invoke(this)
        }
}

