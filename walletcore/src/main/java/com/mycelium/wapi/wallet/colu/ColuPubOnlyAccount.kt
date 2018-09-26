package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputSummary
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
                              , val backing: AccountBacking) : WalletAccount<ColuTransaction, BtcAddress> {
    protected var address: GenericAddress
    protected var uuid: UUID
    @Volatile
    protected var _isSynchronizing: Boolean = false

    protected var _cachedBalance = BalanceSatoshis(0, 0, 0, 0
            , 0, 0, true, true)


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

    override fun getAccountBalance(): Balance = Balance(Value.valueOf(coinType, _cachedBalance.confirmed),
            Value.valueOf(coinType, _cachedBalance.pendingReceiving),
            Value.valueOf(coinType, _cachedBalance.pendingSending),
            Value.valueOf(coinType, _cachedBalance.pendingChange))


    override fun isMineAddress(address: GenericAddress?) = receiveAddress == address

    override fun getTx(transactionId: Sha256Hash?): ColuTransaction? {
        //        checkNotArchived()
        val tex = backing.getTransaction(transactionId)
        val tx = TransactionEx.toTransaction(tex) ?: return null
        return convertTx(tx, tex.height, tex.time)
    }

    override fun getTransactions(offset: Int, limit: Int): List<ColuTransaction> {
        val transactions = backing.getTransactionHistory(offset, limit)
        val result = mutableListOf<ColuTransaction>()
        transactions.forEach { txEx ->
            val tx = TransactionEx.toTransaction(txEx)
            tx?.let {
                result.add(convertTx(it, txEx.height, txEx.time))
            }
        }
        return result
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


        return ColuTransaction(coinType
                , Value.valueOf(coinType, satoshisSent)
                , Value.valueOf(coinType, satoshisReceived)
                , time
                , tx, confirmations, isQueuedOutgoing)
//            , satoshisSent, satoshisReceived, tex.time,
//                    confirmations, isQueuedOutgoing, inputs, outputs, riskAssessmentForUnconfirmedTx.get(tx.id),
//                    tex.binary.size, Value.valueOf(BitcoinMain.get(), Math.abs(satoshisReceived - satoshisSent)))

    }

    override fun getBlockChainHeight(): Int = context.blockHeight


    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSendToRequest(destination: GenericAddress?, amount: Value?): SendRequest<*> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        return false
    }

    override fun canSpend(): Boolean = false

    override fun isArchived() = context.isArchived()

    override fun isActive() = !context.isArchived()

    override fun archiveAccount() = context.setArchived(true)

    override fun activateAccount() = context.setArchived(false)

    override fun dropCachedData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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