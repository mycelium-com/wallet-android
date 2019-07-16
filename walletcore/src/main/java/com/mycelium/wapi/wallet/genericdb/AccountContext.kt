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

