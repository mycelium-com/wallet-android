package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.util.UUID

class TronAccountContext(
    uuid: UUID,
    currency: CryptoCurrency,
    accountName: String,
    balance: Balance,
    val listener: (TronAccountContext) -> Unit,
    val loadListener: (UUID) -> TronAccountContext?,
    val accountIndex: Int,
    enabledTokens: List<String>? = null,
    archived: Boolean = false,
    blockHeight: Int = 0,
    /** Base58 address for watch-only accounts; null when derived from the masterseed. */
    val address: String? = null,
) : AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {

    override fun onChange() {
        listener(this)
    }

    fun updateEnabledTokens() {
        enabledTokens = loadListener(uuid)?.enabledTokens
    }

    var enabledTokens = enabledTokens
        set(value) {
            field = value
            onChange()
        }
}
