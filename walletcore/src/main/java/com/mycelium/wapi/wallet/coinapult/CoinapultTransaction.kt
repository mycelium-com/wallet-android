package com.mycelium.wapi.wallet.coinapult

import com.google.common.base.Optional
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value
import java.io.Serializable


class CoinapultTransaction(val _hash: Sha256Hash, val value: Value, val incoming: Boolean, val completeTime: Long
                           , val state: String, var time: Long, val address: BtcAddress? = null) : GenericTransaction, Serializable {
    override fun getDestinationAddress(): GenericAddress {
        return address as GenericAddress
    }

    var debugInfo: String = ""

    override fun getType(): GenericAssetInfo = value.getType()

    override fun getId(): Sha256Hash? = _hash

    override fun getHashAsString(): String {
        return _hash.toString()
    }

    override fun getHashBytes(): ByteArray {
        return _hash.bytes
    }

    override fun getHeight(): Int = 0

    override fun getConfirmations(): Int = if (state == "complete") 7 else 0

    override fun getTimestamp(): Long = time

    override fun setTimestamp(timestamp: Long) {
        time = timestamp
    }

    override fun isQueuedOutgoing(): Boolean = false

    override fun getConfirmationRiskProfile(): Optional<ConfirmationRiskProfileLocal> = Optional.absent()

    override fun getFee(): Value = Value.zeroValue(type)

    override fun getInputs(): List<GenericTransaction.GenericInput> {
        return listOf()
    }

    override fun getOutputs(): List<GenericTransaction.GenericOutput> {
        return listOf()
    }

    override fun getTransferred(): Value {
        return value
    }

    override fun isIncoming(): Boolean = incoming

    override fun getRawSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
