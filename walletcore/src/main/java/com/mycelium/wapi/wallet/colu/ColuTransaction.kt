package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value


class ColuTransaction(val _type: CryptoCurrency, val _sent: Value, val receive: Value, var time: Int
                      , val tx: Transaction, var confirmation: Int, val _isQueuedOutgoing: Boolean
                      , fee: Value? = null)
    : GenericTransaction {

    override fun getType(): CryptoCurrency = _type

    override fun getHash(): Sha256Hash {
        return tx.id
    }

    override fun getHashAsString(): String = hash.toString()

    override fun getHashBytes(): ByteArray = hash.bytes

    override fun getDepthInBlocks(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setDepthInBlocks(depthInBlocks: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAppearedAtChainHeight(): Int = confirmation

    override fun setAppearedAtChainHeight(appearedAtChainHeight: Int) {
        confirmation = appearedAtChainHeight
    }

    override fun getTimestamp(): Long = time.toLong()

    override fun setTimestamp(timestamp: Int) {
        time = timestamp
    }

    override fun isQueuedOutgoing(): Boolean = _isQueuedOutgoing

    override fun getConfirmationRiskProfile(): Optional<ConfirmationRiskProfileLocal> {
        return Optional.absent()
    }

    override fun getFee(): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getInputs(): MutableList<GenericTransaction.GenericInput> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getOutputs(): MutableList<GenericTransaction.GenericOutput> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSent(): Value = _sent

    override fun getReceived(): Value = receive

    override fun isIncoming() = received.subtract(sent).value > 0

    override fun getRawSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}