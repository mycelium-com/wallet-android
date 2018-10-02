package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bitcoinj.script.ScriptBuilder


class ColuAccount(context: ColuAccountContext, val privateKey: InMemoryPrivateKey
                  , coluCoinType: CryptoCurrency
                  , networkParameters: NetworkParameters
                  , coluNetworkParameters: org.bitcoinj.core.NetworkParameters
                  , coluClient: ColuApi
                  , backing: AccountBacking<ColuTransaction>
                  , listener: AccountListener? = null)
    : ColuPubOnlyAccount(context, privateKey.publicKey, coluCoinType, networkParameters
        , coluNetworkParameters, coluClient, backing, listener), ExportableAccount {

    override fun broadcastOutgoingTransactions(): Boolean {
        return false
    }

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

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun broadcastTx(transaction: ColuTransaction): BroadcastResult {
        if (transaction.tx != null && coluClient.broadcastTx(transaction.tx) != null) {
            return BroadcastResult.SUCCESS
        } else {
            return BroadcastResult.REJECTED
        }
    }

    override fun getSendToRequest(destination: GenericAddress, amount: Value): SendRequest<*> {
        return ColuSendRequest(coinType, destination as BtcAddress, amount)
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun canSpend(): Boolean = true

    override fun checkAmount(receiver: WalletAccount.Receiver?, kbMinerFee: Long, enteredAmount: Value?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getExportData(cipher: KeyCipher?): ExportableAccount.Data {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}