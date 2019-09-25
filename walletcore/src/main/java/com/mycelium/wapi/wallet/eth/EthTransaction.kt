package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericFee
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.crypto.RawTransaction
import org.web3j.protocol.core.methods.response.EthSendTransaction

class EthTransaction(val type: CryptoCurrency, val toAddress: GenericAddress, val value: Value, val gasPrice: GenericFee) : GenericTransaction(type) {
    var rawTransaction: RawTransaction? = null
    var signedHex: String? = null
    var ethSendTransaction: EthSendTransaction? = null
    override fun getId(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun txBytes(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEstimatedTransactionSize() = 21000
}