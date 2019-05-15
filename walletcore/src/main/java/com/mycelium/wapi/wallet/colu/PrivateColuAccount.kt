package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.StandardTransactionBuilder
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.*
import com.mycelium.wapi.model.TransactionOutputSummary
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.colu.json.ColuBroadcastTxHex
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters.ID_MAINNET
import org.bitcoinj.core.NetworkParameters.ID_TESTNET
import org.bitcoinj.script.ScriptBuilder
import java.util.*


class PrivateColuAccount(context: ColuAccountContext, val privateKey: InMemoryPrivateKey
                         , coluCoinType: CryptoCurrency
                         , networkParameters: NetworkParameters
                         , coluClient: ColuApi
                         , accountBacking: AccountBacking<ColuTransaction>
                         , backing: WalletBacking<ColuAccountContext, ColuTransaction>
                         , listener: AccountListener? = null)
    : PublicColuAccount(context, coluCoinType, networkParameters
        , coluClient, accountBacking, backing, listener), ExportableAccount {

    override fun isSyncing(): Boolean {
        //TODO: implement later
        return false
    }

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        return privateKey
    }

    override fun calculateMaxSpendableAmount(minerFeeToUse: Long, destinationAddress: BtcAddress): Value {
        return accountBalance.spendable
    }

    override fun broadcastOutgoingTransactions(): Boolean {
        return false
    }

    override fun completeTransaction(request: SendRequest<ColuTransaction>) {
        if (request is ColuSendRequest) {
            val fromAddresses = mutableListOf(receiveAddress as BtcAddress)
            fromAddresses.addAll(request.fundingAddress)
            val json = coluClient.prepareTransaction(request.destination, fromAddresses, request.amount, request.fee)
            request.baseTransaction = json
            request.isCompleted = true
        } else {
            TODO("completeTransaction not implemented for ${request.javaClass.simpleName}")
        }
    }

    override fun signTransaction(request: SendRequest<ColuTransaction>, keyCipher: KeyCipher) {
        if (!request.isCompleted) {
            return
        }
        if (request is ColuSendRequest) {

            val signTransaction = signTransaction(request.baseTransaction, this)

//            request.tx = signTransaction
            request.transaction = signTransaction
        } else {
            TODO("signTransaction not implemented for ${request.javaClass.simpleName}")
        }
    }

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun signTransaction(txid: ColuBroadcastTxHex.Json?, coluAccount: PrivateColuAccount?): Transaction? {
        if (txid == null) {
//            Log.e(TAG, "signTransaction: No transaction to sign !")
            return null
        }
        if (coluAccount == null) {
//            Log.e(TAG, "signTransaction: No colu account associated to transaction to sign !")
            return null
        }

        // use bitcoinj classes and two methods above to generate signatures
        // and sign transaction
        // then convert to mycelium wallet transaction format
        // Step 1: map to bitcoinj classes

        // DEV only 1 key
        val txBytes: ByteArray?

        try {
            txBytes = Hex.decodeHex(txid.txHex.toCharArray())
        } catch (e: org.apache.commons.codec.DecoderException) {
//            Log.e(TAG, "signTransaction: exception while decoding transaction hex code.")
            return null
        }

        if (txBytes == null) {
//            Log.e(TAG, "signTransaction: failed to decode transaction hex code.")
            return null
        }


        val id =
                when (networkParameters.networkType){
                    NetworkParameters.NetworkType.PRODNET -> ID_MAINNET
                    NetworkParameters.NetworkType.TESTNET -> ID_TESTNET
                    NetworkParameters.NetworkType.REGTEST -> TODO()
                }
        val parameters = org.bitcoinj.core.NetworkParameters.fromID(id)
        val signTx = org.bitcoinj.core.Transaction(parameters, txBytes)

        val privateKeyBytes = coluAccount.getPrivateKey(null).getPrivateKeyBytes()
        val publicKeyBytes = coluAccount.getPrivateKey(null).publicKey.publicKeyBytes
        val ecKey = ECKey.fromPrivateAndPrecalculatedPublic(privateKeyBytes, publicKeyBytes)

        val inputScript = ScriptBuilder.createOutputScript(ecKey.toAddress(parameters))

        for (i in 0 until signTx.inputs.size) {
            val signature = signTx.calculateSignature(i, ecKey, inputScript, org.bitcoinj.core.Transaction.SigHash.ALL, false)
            val scriptSig = ScriptBuilder.createInputScript(signature, ecKey)
            signTx.getInput(i.toLong()).scriptSig = scriptSig
        }

        val signedTransactionBytes = signTx.bitcoinSerialize()
        val signedBitlibTransaction: Transaction
        try {
            signedBitlibTransaction = Transaction.fromBytes(signedTransactionBytes)
        } catch (e: Transaction.TransactionParsingException) {
//            Log.e(TAG, "signTransaction: Error parsing bitcoinj transaction ! msg: " + e.message)
            return null
        }

        return signedBitlibTransaction
    }

    override fun broadcastTx(tx: ColuTransaction): BroadcastResult {
        return if (tx.tx != null && coluClient.broadcastTx(tx.tx) != null) {
            BroadcastResult(BroadcastResultType.SUCCESS)
        } else {
            BroadcastResult(BroadcastResultType.REJECTED)
        }
    }

    override fun getSendToRequest(destination: BtcAddress, amount: Value, feePerKb: Value): SendRequest<ColuTransaction> {
        return ColuSendRequest(coinType, destination, amount, feePerKb)
    }

    override fun getSyncTotalRetrievedTransactions(): Int = 0

    override fun canSpend(): Boolean = true

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        var privKey = Optional.absent<String>()
        val publicDataMap = HashMap<BipDerivationType, String>()
        if (canSpend()) {
            try {
                privKey = Optional.of(this.privateKey.getBase58EncodedPrivateKey(networkParameters))
            } catch (ignore: KeyCipher.InvalidKeyCipher) {
            }

        }
        publicDataMap[BipDerivationType.getDerivationTypeByAddressType(AddressType.P2PKH)] = receiveAddress.toString()
        return ExportableAccount.Data(privKey, publicDataMap)

    }

    fun getUnspentTransactionOutputSummary(): List<TransactionOutputSummary> {
        // Get all unspent outputs for this account
        val outputs = accountBacking.allUnspentOutputs

        // Transform it to a list of summaries
        val list = ArrayList<TransactionOutputSummary>()
        val blockChainHeight = blockChainHeight
        for (output in outputs) {

            val script = ScriptOutput.fromScriptBytes(output.script)
            val address: Address
            address = if (script == null) {
                Address.getNullAddress(networkParameters)
                // This never happens as we have parsed this script before
            } else {
                script.getAddress(networkParameters)
            }
            val confirmations: Int = if (output.height == -1) {
                0
            } else {
                Math.max(0, blockChainHeight - output.height + 1)
            }

            val summary = TransactionOutputSummary(output.outPoint, output.value, output.height, confirmations, address)
            list.add(summary)
        }
        // Sort & return
        list.sort()
        return list
    }

    override fun getTypicalEstimatedTransactionSize(): Int {
        // Colu transaction is a typical bitcoin transaction containing additional inputs and OP_RETURN data
        // Typical Colu transaction has 2 inputs and 4 outputs
        return FeeEstimatorBuilder().setLegacyInputs(2)
                .setLegacyOutputs(4)
                .createFeeEstimator()
                .estimateTransactionSize()
    }

}