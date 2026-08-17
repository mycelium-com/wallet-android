package com.mycelium.wapi.wallet.trc20

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.util.UUID

class TRC20AccountContext(
    uuid: UUID,
    currency: CryptoCurrency,
    accountName: String,
    balance: Balance,
    val listener: (TRC20AccountContext) -> Unit,
    val contractAddress: String,
    val symbol: String,
    val unitExponent: Int,
    val tronAccountId: UUID,
    archived: Boolean = false,
    blockHeight: Int = 0,
) : AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {

    override fun onChange() {
        listener(this)
    }
}
