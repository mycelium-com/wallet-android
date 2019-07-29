package com.mycelium.wapi.wallet.genericdb

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

class AccountContextImpl(uuid: UUID,
                         currency: CryptoCurrency,
                         override var accountName: String,
                         override var balance: Balance,
                         override var archived: Boolean) : AccountContext by
    AccountContext.Impl(uuid, currency, accountName, balance, archived)

