package com.mycelium.wapi.wallet.genericdb

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

interface AccountContext {
    val uuid: UUID
    val currency: CryptoCurrency
    var accountName: String
    var balance: Balance
    var archived: Boolean
}

data class AccountContextImpl(override val uuid: UUID,
                              override val currency: CryptoCurrency,
                              override var accountName: String,
                              override var balance: Balance,
                              override var archived: Boolean): AccountContext

