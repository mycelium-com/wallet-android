package com.mycelium.wapi.wallet.eth

import com.mycelium.generated.wallet.database.AccountContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.util.*

class EthAccountContext(uuid: UUID,
                        currency: CryptoCurrency,
                        accountName: String,
                        balance: Balance,
                        listener: (AccountContext) -> Unit,
                        archived: Boolean = false) :
        AccountContext by AccountContextImpl(uuid, currency, accountName, balance, listener, archived) {

}