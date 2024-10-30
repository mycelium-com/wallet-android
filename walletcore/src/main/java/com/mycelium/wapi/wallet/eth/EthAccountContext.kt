package com.mycelium.wapi.wallet.eth

import com.mycelium.generated.wallet.database.EthContext
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.math.BigInteger
import java.util.*

class EthAccountContext(uuid: UUID,
                        currency: CryptoCurrency,
                        accountName: String,
                        balance: Balance,
                        val listener: (EthAccountContext) -> Unit,
                        val loadListener: (UUID) -> EthAccountContext?,
                        val accountIndex: Int,
                        enabledTokens: List<String>? = null,
                        archived: Boolean = false,
                        blockHeight: Int = 0,
                        nonce: BigInteger = BigInteger.ZERO) :
        AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {

    fun ethContext() = EthContext(uuid, nonce, enabledTokens, accountIndex)

    override fun onChange() {
        listener(this)
    }

    fun updateEnabledTokens() {
        enabledTokens = loadListener(uuid)?.enabledTokens
    }

    var nonce = nonce
        set(value) {
            field = value
            onChange()
        }
    var enabledTokens = enabledTokens
        set(value) {
            field = value
            onChange()
        }
}