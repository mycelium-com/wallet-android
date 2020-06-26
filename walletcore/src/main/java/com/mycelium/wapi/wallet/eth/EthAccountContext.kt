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
                        listener: (EthAccountContext) -> Unit,
                        override val accountIndex: Int,
                        enabledTokens: List<String>? = null,
                        archived: Boolean = false,
                        blockHeight: Int = 0,
                        nonce: BigInteger = BigInteger.ZERO) :
        EthContext by EthContext.Impl(uuid, nonce, enabledTokens, accountIndex),
        AccountContextImpl<EthAccountContext>(uuid, currency, accountName, balance, listener, archived, blockHeight) {
    override var nonce = nonce
        set(value) {
            field = value
            listener.invoke(this)
        }
    override var enabledTokens = enabledTokens
        set(value) {
            field = value
            listener.invoke(this)
        }
}