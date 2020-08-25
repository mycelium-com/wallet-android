package com.mycelium.wapi.wallet.fio

import com.mycelium.generated.wallet.database.FioContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.util.*

class FioAccountContext(override val uuid: UUID,
                        currency: CryptoCurrency,
                        accountName: String,
                        balance: Balance,
                        listener: (FioAccountContext) -> Unit,
                        override val accountIndex: Int,
                        archived: Boolean = false,
                        blockHeight: Int = 0) :
        FioContext by FioContext.Impl(uuid, accountIndex),
        AccountContextImpl<FioAccountContext>(uuid, currency, accountName, balance, listener, archived, blockHeight)