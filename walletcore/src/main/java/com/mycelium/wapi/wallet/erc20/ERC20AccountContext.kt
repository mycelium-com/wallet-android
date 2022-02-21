package com.mycelium.wapi.wallet.erc20

import com.mycelium.generated.wallet.database.Erc20Context
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.math.BigInteger
import java.util.*

class ERC20AccountContext(override val uuid: UUID,
                          currency: CryptoCurrency,
                          accountName: String,
                          balance: Balance,
                          val listener: (ERC20AccountContext) -> Unit,
                          override val contractAddress: String,
                          override val symbol: String,
                          override val unitExponent: Int,
                          override val ethAccountId: UUID,
                          archived: Boolean = false,
                          blockHeight: Int = 0,
                          nonce: BigInteger = BigInteger.ZERO) :
        Erc20Context by Erc20Context.Impl(uuid, nonce, contractAddress, unitExponent, symbol, ethAccountId),
        AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {
    override fun onChange() {
        listener(this)
    }

    override var nonce = nonce
        set(value) {
            field = value
            onChange()
        }
}