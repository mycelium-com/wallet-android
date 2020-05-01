package com.mycelium.wapi.wallet.erc20

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import org.web3j.tx.Transfer
import java.math.BigInteger

class Erc20Transaction(val coinType: ERC20Token, val toAddress: GenericAddress, val value: Value, val gasPrice: BigInteger,
                       val gasLimit: BigInteger) : GenericTransaction(coinType) {
    var txHash: ByteArray? = null
    var txBinary: ByteArray? = null
    override fun getEstimatedTransactionSize() = Transfer.GAS_LIMIT.toInt()

    override fun getId() = txHash

    override fun txBytes() = txBinary
}
