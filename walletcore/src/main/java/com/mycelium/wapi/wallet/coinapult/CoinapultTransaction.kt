package com.mycelium.wapi.wallet.coinapult

import com.google.common.base.Optional
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.coins.Value


class CoinapultTransaction(val value: Value, val incoming: Boolean) : GenericTransaction {
    override fun getType(): GenericAssetInfo = value.getType()

    override fun getHash(): Sha256Hash {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHashAsString(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getHashBytes(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDepthInBlocks(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setDepthInBlocks(depthInBlocks: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAppearedAtChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAppearedAtChainHeight(appearedAtChainHeight: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTimestamp(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTimestamp(timestamp: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isQueuedOutgoing(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getConfirmationRiskProfile(): Optional<ConfirmationRiskProfileLocal> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun getSent(): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceived(): Value {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isIncoming(): Boolean = incoming

    override fun getRawSize(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
