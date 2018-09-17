package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.*
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.model.TransactionOutputSummary
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.BtcSendRequest
import com.mycelium.wapi.wallet.coins.*
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bitcoinj.script.ScriptBuilder
import java.nio.ByteBuffer
import java.util.*


class ColuAccount(val context: ColuAccountContext, val privateKey: InMemoryPrivateKey
                  , val networkParameters: NetworkParameters
                  , val coluNetworkParameters: org.bitcoinj.core.NetworkParameters
                  , val coluClient: ColuApi
                  , val backing: AccountBacking) : WalletAccount<ColuTransaction, BtcAddress> {

    private var address: GenericAddress
    private var uuid: UUID
    @Volatile
    private var _isSynchronizing: Boolean = false

    private var _cachedBalance = BalanceSatoshis(0, 0, 0, 0
            , 0, 0, true, true)

    init {
        val address1 = privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
        address = BtcAddress.from(ColuMain, address1.toString())
        uuid = getGuidForAsset(ColuMain, address1.allAddressBytes)
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


    override fun getId(): UUID {
        return uuid
    }

//    override fun getTransactionDetails(txid: Sha256Hash?): TransactionDetails {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override fun getReceiveAddress(): GenericAddress = address

    override fun isMineAddress(address: GenericAddress?): Boolean {
        return receiveAddress == address
    }

    override fun getTx(transactionId: Sha256Hash?): ColuTransaction? {
//        checkNotArchived()
        val tex = backing.getTransaction(transactionId)
        val tx = TransactionEx.toTransaction(tex) ?: return null

        var satoshisReceived: Long = 0
        var satoshisSent: Long = 0
        val outputs = ArrayList<GenericTransaction.GenericOutput>()
        for (output in tx.outputs) {
            val address = output.script.getAddress(networkParameters)
            if (isMineAddress(BtcAddress(ColuMain, address.allAddressBytes)/*output.script*/)) {
                satoshisReceived += output.value
            }
            if (address != null && address !== Address.getNullAddress(networkParameters)) {
                val currency = if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get()
                outputs.add(GenericTransaction.GenericOutput(BtcAddress(currency, address.allAddressBytes), Value.valueOf(coinType, output.value)))
            }
        }
        val inputs = ArrayList<GenericTransaction.GenericInput>() //need to create list of outputs

        // Inputs
        if (!tx.isCoinbase) {
            for (input in tx.inputs) {
                // find parent output
                val funding = backing.getParentTransactionOutput(input.outPoint)
                        ?: //                    _logger.logError("Unable to find parent output for: " + input.outPoint)
                        continue
//                if (isMineAddress(funding)) {
//                    satoshisSent += funding.value
//                }
                val address = ScriptOutput.fromScriptBytes(funding.script)!!.getAddress(networkParameters)
                val currency = if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get()
                inputs.add(GenericTransaction.GenericInput(BtcAddress(currency, address.allAddressBytes), Value.valueOf(coinType, funding.value)))
            }
        }

        val confirmations: Int
        if (tex.height == -1) {
            confirmations = 0
        } else {
            confirmations = Math.max(0, blockChainHeight - tex.height + 1)
        }
        val isQueuedOutgoing = backing.isOutgoingTransaction(tx.id)
        return ColuTransaction(coinType, tx)

    }


    override fun getTransactions(offset: Int, limit: Int): MutableList<GenericTransaction> {

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isActive() = !context.isArchived()

    override fun broadcastOutgoingTransactions(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

//    override fun deleteTransaction(transactionId: Sha256Hash?): Boolean {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun getBalance(): BalanceSatoshis {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }

    override fun completeAndSignTx(request: SendRequest<ColuTransaction>) {
        completeTransaction(request)
        signTransaction(request)
//        if (txid == null) {
//            Log.e(TAG, "signTransaction: No transaction to sign !")
//            return null
//        }
//        if (coluAccount == null) {
//            Log.e(TAG, "signTransaction: No colu account associated to transaction to sign !")
//            return null
//        }

        // use bitcoinj classes and two methods above to generate signatures
        // and sign transaction
        // then convert to mycelium wallet transaction format
        // Step 1: map to bitcoinj classes

        // DEV only 1 key
    }

    override fun completeTransaction(request: SendRequest<ColuTransaction>) {
//        val coluSendRequest = request as ColuSendRequest
//        val receivers = ArrayList<WalletAccount.Receiver>()
//        receivers.add(WalletAccount.Receiver(coluSendRequest.destination, coluSendRequest.amount.value))
////        btcSendRequest.unsignedTx = createUnsignedTransaction(receivers, request.fee.value)
    }

    override fun signTransaction(request: SendRequest<ColuTransaction>) {
        if (!request.isCompleted) {
            return
        }

        val coluSendRequest = request as ColuSendRequest
        val txBytes: ByteArray?
        try {
            txBytes = Hex.decodeHex(coluSendRequest.txHex?.toCharArray())
        } catch (e: org.apache.commons.codec.DecoderException) {
            return
        }
        if (txBytes == null) {
            return
        }

        val signTx = org.bitcoinj.core.Transaction(coluNetworkParameters, Hex.decodeHex(request.txHex?.toCharArray()))

        val privateKeyBytes = privateKey.privateKeyBytes
        val publicKeyBytes = privateKey.publicKey.publicKeyBytes
        val ecKey = ECKey.fromPrivateAndPrecalculatedPublic(privateKeyBytes, publicKeyBytes)

        val inputScript = ScriptBuilder.createOutputScript(ecKey.toAddress(coluNetworkParameters))

        for (i in 0 until signTx.inputs.size) {
            val signature = signTx.calculateSignature(i, ecKey, inputScript, org.bitcoinj.core.Transaction.SigHash.ALL, false)
            val scriptSig = ScriptBuilder.createInputScript(signature, ecKey)
            signTx.getInput(i.toLong()).scriptSig = scriptSig
        }

        val signedTransactionBytes = signTx.bitcoinSerialize()
        try {
            coluSendRequest.setTransaction(Transaction.fromBytes(signedTransactionBytes))
        } catch (e: Transaction.TransactionParsingException) {
            return
        }
    }

    override fun archiveAccount() = context.setArchived(true)

    override fun getAccountBalance(): Balance = Balance(Value.valueOf(coinType, _cachedBalance.confirmed),
            Value.valueOf(coinType, _cachedBalance.pendingReceiving),
            Value.valueOf(coinType, _cachedBalance.pendingSending),
            Value.valueOf(coinType, _cachedBalance.pendingChange))


    override fun getUnspentTransactionOutputSummary(): MutableList<TransactionOutputSummary> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isDerivedFromInternalMasterseed(): Boolean = true

    override fun getCoinType(): CryptoCurrency = ColuMain

    override fun getBlockChainHeight(): Int = context.blockHeight

    override fun dropCachedData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(transaction: ColuTransaction): BroadcastResult {
        if (coluClient.broadcastTransaction(transaction.tx) != null) {
            return BroadcastResult.SUCCESS
        } else {
            return BroadcastResult.REJECTED
        }
    }

    override fun getSendToRequest(destination: GenericAddress?, amount: Value?): SendRequest<*> {
        return BtcSendRequest.to(destination as BtcAddress, amount) //TODO change to Colu Send Request
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun canSpend(): Boolean = true

    override fun isArchived() = context.isArchived()

    override fun activateAccount() = context.setArchived(false)

    override fun isSynchronizing() = _isSynchronizing

    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isVisible(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}