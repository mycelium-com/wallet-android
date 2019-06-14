package com.mycelium.wapi.wallet.colu

import com.google.common.base.Preconditions
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.CommonAccountBacking
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.GenericTransactionSummary
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import java.util.*

class InMemoryColuWalletManagerBacking : ColuWalletManagerBacking<ColuAccountContext> {

    private val _values = HashMap<String, ByteArray>()
    private val _backings = HashMap<UUID, InMemoryColuAccountBacking>()
    private val _contexts = HashMap<UUID, ColuAccountContext>()
    private val _coluAccountContexts = HashMap<UUID, ColuAccountContext>()
    private var maxSubId = 0

    override fun loadAccountContexts(): MutableList<ColuAccountContext> {
        val list = mutableListOf<ColuAccountContext>()
        for (c in _contexts.values) {
            list.add(c)
        }
        return list
    }

    override fun getAccountBacking(accountId: UUID): CommonAccountBacking {
        val backing = _backings[accountId]
        Preconditions.checkNotNull<InMemoryColuAccountBacking>(backing)
        return backing!!
    }

    override fun createAccountContext(context: ColuAccountContext) {
        _contexts[context.id] = context
        _backings[context.id] = InMemoryColuAccountBacking()
    }

    override fun updateAccountContext(context: ColuAccountContext?) {}

    override fun deleteAccountContext(uuid: UUID?) {
        _backings.remove(uuid)
        _coluAccountContexts.remove(uuid)
    }

    private fun idToString(id: ByteArray?): String {
        return HexUtils.toHex(id)
    }

    private fun idToString(id: ByteArray?, subId: Int): String {
        return "sub" + subId + "." + HexUtils.toHex(id)
    }

    override fun getValue(id: ByteArray?) = _values[idToString(id)]

    override fun getValue(id: ByteArray?, subId: Int): ByteArray? {
        if (subId > maxSubId) {
            throw RuntimeException("subId does not exist")
        }
        return _values[idToString(id, subId)]
    }

    override fun setValue(id: ByteArray?, plaintextValue: ByteArray) {
        _values.put(idToString(id), plaintextValue);
    }

    override fun getMaxSubId() = maxSubId


    override fun setValue(key: ByteArray?, subId: Int, value: ByteArray) {
        if (subId > maxSubId) {
            maxSubId = subId
        }
        _values[idToString(key, subId)] = value
    }

    override fun deleteValue(id: ByteArray?) {
        _values.remove(idToString(id))
    }

    override fun deleteSubStorageId(subId: Int) {
        throw UnsupportedOperationException()
    }

    private class InMemoryColuAccountBacking : ColuAccountBacking {
        override fun saveLastFeeEstimation(feeEstimation: FeeEstimationsGeneric?, assetType: GenericAssetInfo?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun loadLastFeeEstimation(assetType: GenericAssetInfo?): FeeEstimationsGeneric {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        private val _unspentOuputs = HashMap<OutPoint, TransactionOutputEx>()
        private val _transactions = HashMap<Sha256Hash, GenericTransactionSummary>()
        private val _parentOutputs = HashMap<OutPoint, TransactionOutputEx>()
        private val _outgoingTransactions = HashMap<Sha256Hash, ByteArray>()

        override fun clear() {
            _unspentOuputs.clear()
            _transactions.clear()
            _parentOutputs.clear()
            _outgoingTransactions.clear()
        }

        override fun putUnspentOutputs(unspentOutputs: MutableList<TransactionOutputEx>?) {
            unspentOutputs?.forEach {_unspentOuputs[it.outPoint] = it}

        }

        override fun beginTransaction() {

        }

        override fun setTransactionSuccessful() {

        }

        override fun getTransactionSummaries(offset: Int, length: Int): MutableList<GenericTransactionSummary> = _transactions.values.toMutableList()

        override fun getTransactionsSince(receivingSince: Long): MutableList<GenericTransactionSummary> {
            val list = mutableListOf<GenericTransactionSummary>()
            _transactions.values.forEach {
                if (it.time >= receivingSince) {
                    list.add(it)
                }
            }
            return list
        }

        override fun endTransaction() {

        }

        override fun putTransactions(transactionSummaries: MutableList<GenericTransactionSummary>?) {
            transactionSummaries?.forEach { _transactions[Sha256Hash(it.id)] = it }
        }

        override fun getUnspentOutputs(): MutableList<TransactionOutputEx> {
            return LinkedList(_unspentOuputs.values)
        }

        override fun getTxSummary(txId: Sha256Hash?) = _transactions[txId]

    }

}