package com.mycelium.wapi.wallet.eth

import com.mycelium.generated.wallet.database.EthContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.math.BigInteger
import java.util.*

class EthAccountContext(override val uuid: UUID,
                        currency: CryptoCurrency,
                        accountName: String,
                        balance: Balance,
                        val listener: (EthAccountContext) -> Unit,
                        val loadListener: (UUID) -> EthAccountContext?,
                        override val accountIndex: Int,
                        enabledTokens: List<String>? = null,
                        archived: Boolean = false,
                        blockHeight: Int = 0,
                        nonce: BigInteger = BigInteger.ZERO) :
        EthContext by EthContext.Impl(uuid, nonce, enabledTokens, accountIndex),
        AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {
    override fun onChange() {
        listener(this)
    }

    fun updateEnabledTokens() {
        enabledTokens = loadListener(uuid)?.enabledTokens
    }

    override var nonce = nonce
        set(value) {
            field = value
            onChange()
        }
    override var enabledTokens = enabledTokens
        set(value) {
            field = value
            onChange()
        }
}