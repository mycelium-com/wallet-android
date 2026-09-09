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
                          nonce: BigInteger = BigInteger.ZERO,
                          everFullySynced: Boolean = false) :
        AccountContextImpl(uuid, currency, accountName, balance, archived, blockHeight) {

    fun erc20Context() =
        Erc20Context(uuid, nonce, contractAddress, unitExponent, symbol, ethAccountId, everFullySynced)

    override fun onChange() {
        listener(this)
    }

    var nonce = nonce
        set(value) {
            field = value
            onChange()
        }

    /**
     * True once this token account has walked the whole address history at least
     * once. Tracked per token account on purpose: the parent EthAccount's flag
     * says nothing about whether *this* token's transfers have been fetched, and
     * keying off it left freshly added (or re-activated) token accounts stuck at
     * a zero balance forever.
     */
    var everFullySynced = everFullySynced
        set(value) {
            field = value
            onChange()
        }
}