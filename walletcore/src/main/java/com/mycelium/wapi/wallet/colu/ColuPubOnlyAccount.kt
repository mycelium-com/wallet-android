package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


open class ColuPubOnlyAccount(val context: ColuAccountContext, val publicKey: PublicKey
                              , val coluCoinType: CryptoCurrency
                              , val networkParameters: NetworkParameters
                              , val coluNetworkParameters: org.bitcoinj.core.NetworkParameters
                              , val coluClient: ColuApi
                              , val backing: AccountBacking<ColuTransaction>
                              , val listener: AccountListener? = null) : WalletAccount<ColuTransaction, BtcLegacyAddress> {

    protected var address: GenericAddress
    protected var uuid: UUID
    @Volatile
    protected var _isSynchronizing: Boolean = false

    protected var cachedBalance = Balance(Value.zeroValue(coinType), Value.zeroValue(coinType)
            , Value.zeroValue(coinType), Value.zeroValue(coinType))


    init {
        val address1 = publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
        address = AddressUtils.from(coluCoinType, address1.toString())
        uuid = ColuUtils.getGuidForAsset(coluCoinType, address1.allAddressBytes)
        cachedBalance = calculateBalance(backing.getTransactions(0, 2000))
    }

    override fun getId(): UUID = uuid

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress(): GenericAddress = address

    override fun getCoinType(): CryptoCurrency = coluCoinType

    override fun getAccountBalance(): Balance = cachedBalance


    override fun isMineAddress(address: GenericAddress?) = receiveAddress == address

    override fun getTx(transactionId: Sha256Hash): ColuTransaction? {
        //        checkNotArchived()
        return backing.getTx(transactionId)
    }

    override fun getTransactions(offset: Int, limit: Int): List<ColuTransaction> {
        return backing.getTransactions(offset, limit)
    }

    override fun getBlockChainHeight(): Int = context.blockHeight


    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSendToRequest(destination: BtcAddress, amount: Value): SendRequest<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Synchronized
    override fun synchronize(mode: SyncMode?): Boolean {
//        Log.e(TAG, "getBalances: address=" + address.get().toString())


        // collect all tx history at that address from mycelium wapi server (non colored)
//        val allTxidList = LinkedList<com.mrd.bitlib.util.Sha256Hash>()

//        val wapiClient = getWapi()
//        if (wapiClient == null) {
//            Log.e(TAG, "getTransactionSummaries: wapiClient not found !")
//            return
//        }

        // retrieve history from colu server
        val transactions = coluClient.getAddressTransactions(receiveAddress)
        transactions?.let {
            backing.putTransactions(transactions)
            cachedBalance = calculateBalance(transactions)
            listener?.balanceUpdated(this)
        }


//        if (addressInfoWithTransactions.transactions != null && addressInfoWithTransactions!!.transactions.size > 0) {
//            account.setHistory(addressInfoWithTransactions!!.transactions)
//            for (historyTx in addressInfoWithTransactions!!.transactions) {
//                allTxidList.add(com.mrd.bitlib.util.Sha256Hash.fromString(historyTx.txid))
//            }
//        }

//        try {
//            val unspentOutputResponse = wapiClient!!.queryUnspentOutputs(QueryUnspentOutputsRequest(Wapi.VERSION, account.getSendingAddresses()))
//                    .getResult()
//            account.setBlockChainHeight(unspentOutputResponse.height)
//        } catch (e: WapiException) {
//            Log.w(TAG, "Warning ! Error accessing unspent outputs response: " + e.message)
//        }
//
//
//        account.setUtxos(addressInfoWithTransactions!!.utxos)

        // start additional code to retrieve extended info from wapi server
//        val trRequest = GetTransactionsRequest(2, allTxidList)
//        val wapiResponse = wapiClient!!.getTransactions(trRequest)
//        var trResponse: GetTransactionsResponse? = null
//        if (wapiResponse == null) {
//            return
//        }
//        try {
//            trResponse = wapiResponse!!.getResult()
//        } catch (e: Exception) {
//            Log.w(TAG, "Warning ! Error accessing transaction response: " + e.message)
//        }


//        if (trResponse != null && trResponse.transactions != null) {
//            account.setHistoryTxInfos(trResponse.transactions)
//        }
        return true
    }

    private fun calculateBalance(transactions: List<ColuTransaction>): Balance {
        var confirmed = Value.zeroValue(coinType)
        var receiving = Value.zeroValue(coinType)
        var sending = Value.zeroValue(coinType)

        for (tx in transactions) {
            tx.inputs.forEach {
                if (tx.depthInBlocks < 6 && isMineAddress(it.address)) {
                    sending = sending.add(it.value)
                } else if (isMineAddress(it.address)) {
                    confirmed = confirmed.subtract(it.value)
                }
            }
            tx.outputs.forEach {
                if (tx.depthInBlocks < 6 && isMineAddress(it.address)) {
                    receiving = receiving.add(it.value)
                } else if (isMineAddress(it.address)) {
                    confirmed = confirmed.add(it.value)
                }
            }

        }
        return Balance(confirmed, receiving, sending, Value.zeroValue(coinType))
    }


    override fun canSpend(): Boolean = false

    override fun isArchived() = context.isArchived()

    override fun isActive() = !context.isArchived()

    override fun archiveAccount() = context.setArchived(true)

    override fun activateAccount() = context.setArchived(false)

    override fun dropCachedData() {
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed(): Boolean = false

    override fun isSynchronizing() = _isSynchronizing

    override fun broadcastOutgoingTransactions(): Boolean = false

    override fun getSyncTotalRetrievedTransactions(): Int {
        return 0;
    }

    override fun completeAndSignTx(request: SendRequest<ColuTransaction>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun completeTransaction(request: SendRequest<ColuTransaction>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun signTransaction(request: SendRequest<ColuTransaction>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(tx: ColuTransaction): BroadcastResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}