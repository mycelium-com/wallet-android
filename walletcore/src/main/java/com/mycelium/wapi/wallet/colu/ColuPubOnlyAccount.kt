package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.*
import java.nio.ByteBuffer
import java.util.*


open class ColuPubOnlyAccount(val context: ColuAccountContext, val publicKey: PublicKey
                              , val coluCoinType: CryptoCurrency
                              , val networkParameters: NetworkParameters
                              , val coluNetworkParameters: org.bitcoinj.core.NetworkParameters
                              , val coluClient: ColuApi
                              , val backing: AccountBacking<ColuTransaction>
                              , val listener: AccountListener? = null) : WalletAccount<ColuTransaction, BtcAddress> {

    override fun getAccountDefaultCurrency(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    protected var address: GenericAddress
    protected var uuid: UUID
    @Volatile
    protected var _isSynchronizing: Boolean = false

    protected var _cachedBalance = BalanceSatoshis(0, 0, 0, 0
            , 0, 0, true, true)

    protected var cachedBalance = Balance(Value.zeroValue(coinType), Value.zeroValue(coinType)
            , Value.zeroValue(coinType), Value.zeroValue(coinType))


    init {
        val address1 = publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
        address = AddressUtils.from(coluCoinType, address1.toString())
        uuid = getGuidForAsset(coluCoinType, address1.allAddressBytes)
    }

    override fun getId(): UUID {
        return uuid
    }

    private fun getGuidForAsset(cryptoCurrency: CryptoCurrency, addressBytes: ByteArray): UUID {
        val byteWriter = ByteWriter(36)
        byteWriter.putBytes(addressBytes)
        byteWriter.putRawStringUtf8(cryptoCurrency.id)
        val accountId = HashUtils.sha256(byteWriter.toBytes())
        return getGuidFromByteArray(accountId.bytes)
    }

    private fun getGuidFromByteArray(bytes: ByteArray): UUID {
        val bb = ByteBuffer.wrap(bytes)
        val high = bb.long
        val low = bb.long
        return UUID(high, low)
    }

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReceiveAddress(): GenericAddress = address

    override fun getCoinType(): CryptoCurrency = coluCoinType

    override fun getAccountBalance(): Balance = cachedBalance


    override fun isMineAddress(address: GenericAddress?) = receiveAddress == address

    override fun getTx(transactionId: Sha256Hash?): ColuTransaction? {
        //        checkNotArchived()
        return backing.getTx(transactionId)
    }

    override fun getTransactions(offset: Int, limit: Int): List<ColuTransaction> {
        return backing.getTransactions(offset, limit)
    }

    private fun convertTx(tx: Transaction, height: Int, time: Int): ColuTransaction {

        var satoshisReceived: Long = 0
        val outputs = mutableListOf<GenericTransaction.GenericOutput>() //need to create list of outputs
        for (output in tx.outputs) {
            val address = output.script.getAddress(networkParameters)
            if (isMineAddress(BtcAddress(coluCoinType, output.script.getAddress(networkParameters).allAddressBytes))) {
                satoshisReceived += output.value
            }
            if (address != null && address != Address.getNullAddress(networkParameters)) {
                outputs.add(GenericTransaction.GenericOutput(BtcAddress(coinType, address.allAddressBytes), Value.valueOf(coinType, output.value)))
            }
        }

        var satoshisSent: Long = 0
        val inputs = ArrayList<GenericTransaction.GenericInput>() //need to create list of outputs
        // Inputs
        if (!tx.isCoinbase) {
            for (input in tx.inputs) {
                // find parent output
                val funding = backing.getParentTransactionOutput(input.outPoint)
                if (funding == null) {
//                        _logger.logError("Unable to find parent output for: " + input.outPoint)
                    continue
                }
                val script = ScriptOutput.fromScriptBytes(funding.script)
                if (isMineAddress(BtcAddress(coinType, script.getAddress(networkParameters).allAddressBytes))) {
                    satoshisSent += funding.value
                }

                val address = ScriptOutput.fromScriptBytes(funding.script)!!.getAddress(networkParameters)
                val currency = if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get()
                inputs.add(GenericTransaction.GenericInput(BtcAddress(currency, address.allAddressBytes), Value.valueOf(coinType, funding.value)))
            }
        }

        val confirmations: Int
        if (height == -1) {
            confirmations = 0
        } else {
            confirmations = Math.max(0, blockChainHeight - height + 1)
        }

        val isQueuedOutgoing = backing.isOutgoingTransaction(tx.id)


        return ColuTransaction(tx.id, coinType
                , Value.valueOf(coinType, satoshisSent)
                , Value.valueOf(coinType, satoshisReceived)
                , time
                , tx, confirmations, isQueuedOutgoing
                , inputs, outputs)
//            , satoshisSent, satoshisReceived, tex.time,
//                    confirmations, isQueuedOutgoing, inputs, outputs, riskAssessmentForUnconfirmedTx.get(tx.id),
//                    tex.binary.size, Value.valueOf(BitcoinMain.get(), Math.abs(satoshisReceived - satoshisSent)))

    }

    override fun getBlockChainHeight(): Int = context.blockHeight


    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSendToRequest(destination: GenericAddress, amount: Value): SendRequest<*> {
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
        backing.putTransactions(transactions)

        cachedBalance = calculateBalance(transactions)
        listener?.balanceUpdated(this)


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
//            storeColuBalance(account.getUuid(), assetConfirmedBalance.toString())
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