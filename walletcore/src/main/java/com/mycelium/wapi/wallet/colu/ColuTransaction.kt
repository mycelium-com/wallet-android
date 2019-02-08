package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable


class ColuTransaction(val id: Sha256Hash, val _type: CryptoCurrency, val _transferred: Value, var time: Long,
                      val tx: Transaction?, val _height: Int, var _confirmations: Int, val _isQueuedOutgoing: Boolean
                      , val input: List<GenericTransaction.GenericInput>, val output: List<GenericTransaction.GenericOutput>
                      , fee: Value? = null)
    : GenericTransaction, Serializable {

    override fun getHeight(): Int = _height

    override fun getConfirmations(): Int = _confirmations

    override fun getType(): GenericAssetInfo = _type

    override fun getHash(): Sha256Hash {
        return id
    }

    override fun getHashAsString(): String = hash.toString()

    override fun getHashBytes(): ByteArray = hash.bytes

    override fun getTimestamp(): Long = time

    override fun setTimestamp(timestamp: Long) {
        time = timestamp
    }

    override fun isQueuedOutgoing(): Boolean = _isQueuedOutgoing

    override fun getConfirmationRiskProfile(): Optional<ConfirmationRiskProfileLocal> {
        return Optional.absent()
    }

    override fun getFee(): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInputs(): List<GenericTransaction.GenericInput> {
        return input
    }

    override fun getOutputs(): List<GenericTransaction.GenericOutput> {
        return output
    }

    override fun getTransferred(): Value = _transferred

    override fun isIncoming() = transferred.value > 0

    override fun getRawSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}