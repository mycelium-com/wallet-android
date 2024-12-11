package com.mycelium.wapi.wallet.erc20

import com.mycelium.generated.wallet.database.Erc20Context
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import java.math.BigInteger
import java.util.*

class ERC20AccountContext(uuid: UUID,
                          currency: CryptoCurrency,
                          accountName: String,
                          balance: Balance,
                          val listener: (ERC20AccountContext) -> Unit,
                          val contractAddress: String,
                          val symbol: String,
                          val unitExponent: Int,
                          val ethAccountId: UUID,
                          archived: Boolean = false,
                          blockHeight: Int = 0,
                          nonce: BigInteger = BigInteger.ZERO) :
        AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {

    fun erc20Context() =
        Erc20Context(uuid, nonce, contractAddress, unitExponent, symbol, ethAccountId)

    override fun onChange() {
        listener(this)
    }

    var nonce = nonce
        set(value) {
            field = value
            onChange()
        }
}