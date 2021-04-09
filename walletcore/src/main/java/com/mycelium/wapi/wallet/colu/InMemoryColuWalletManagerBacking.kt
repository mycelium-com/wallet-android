package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.TransactionOutputEx
import com.mycelium.wapi.wallet.CommonAccountBacking
import com.mycelium.wapi.wallet.FeeEstimationsGeneric
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.colu.json.Tx
import java.util.*

class InMemoryColuWalletManagerBacking : ColuWalletManagerBacking<ColuAccountContext> {
    private val _values = HashMap<String, ByteArray>()
    private val _backings = HashMap<UUID, InMemoryColuAccountBacking>()
    private val _contexts = HashMap<UUID, ColuAccountContext>()
    private val _coluAccountContexts = HashMap<UUID, ColuAccountContext>()
    private var maxSubId = 0

    override fun loadAccountContexts(): MutableList<ColuAccountContext> = _contexts.values.toMutableList()

    override fun getAccountBacking(accountId: UUID): CommonAccountBacking = _backings[accountId]!!

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
        _values[idToString(id)] = plaintextValue
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

        private val _unspentOuputs = HashMap<OutPoint, TransactionOutputEx>()
        private val _transactions = HashMap<Sha256Hash, Tx.Json>()
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

        override fun getTransactions(offset: Int, length: Int): MutableList<Tx.Json> = _transactions.values.toMutableList()

        override fun getTransactionsSince(receivingSince: Long): MutableList<Tx.Json> {
            val list = mutableListOf<Tx.Json>()
            _transactions.values.forEach {
                if (it.time >= receivingSince) {
                    list.add(it)
                }
            }
            return list
        }

        override fun endTransaction() {

        }

        override fun putTransactions(transactions: MutableList<Tx.Json>?) {

            transactions?.forEach { _transactions[Sha256Hash.fromString(it.txid)] = it }
        }

        override fun getUnspentOutputs(): MutableList<TransactionOutputEx> {
            return LinkedList(_unspentOuputs.values)
        }

        override fun getTx(txId: Sha256Hash?) = _transactions[txId]

    }

}