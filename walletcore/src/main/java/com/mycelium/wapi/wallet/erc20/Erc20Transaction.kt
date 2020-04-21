package com.mycelium.wapi.wallet.erc20

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import org.web3j.tx.Transfer
import java.math.BigInteger

class Erc20Transaction(val coinType: ERC20Token, val toAddress: Address, val value: Value, val gasPrice: BigInteger,
                       val gasLimit: BigInteger) : Transaction(coinType) {
    var txHash: ByteArray? = null
    var txBinary: ByteArray? = null
    override fun getEstimatedTransactionSize() = Transfer.GAS_LIMIT.toInt()

    override fun getId() = txHash

    override fun txBytes() = txBinary
}
