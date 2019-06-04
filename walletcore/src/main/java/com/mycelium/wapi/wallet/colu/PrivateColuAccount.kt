package com.mycelium.wapi.wallet.colu

import com.google.common.base.Optional
import com.mrd.bitlib.FeeEstimatorBuilder
import com.mrd.bitlib.crypto.*
import com.mrd.bitlib.model.*
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.FeePerKbFee
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
                         , accountBacking: ColuAccountBacking
                         , backing: WalletBacking<ColuAccountContext>
                         , listener: AccountListener? = null
                         , wapi: Wapi)
    : PublicColuAccount(context, coluCoinType, networkParameters
        , coluClient, accountBacking, backing, listener, wapi), ExportableAccount {

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

    override fun createTx(address: GenericAddress?, amount: Value?, fee: GenericFee?): GenericTransaction? {
        val feePerKb = (fee as FeePerKbFee).feePerKb
        val coluTx = ColuTransaction(coinType, address as BtcAddress, amount!!, feePerKb)
        val fromAddresses = mutableListOf(receiveAddress as BtcAddress)
        val json = coluClient.prepareTransaction(coluTx.destination, fromAddresses, coluTx.amount, feePerKb)
        coluTx.baseTransaction = json
        return coluTx
    }

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        if (request is ColuTransaction) {
            val signTransaction = signTransaction(request.baseTransaction, this)
            request.transaction = signTransaction
        } else {
            TODO("signTx not implemented for ${request.javaClass.simpleName}")
        }
    }

    override fun setAllowZeroConfSpending(allowZeroConfSpending: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun signTransaction(txid: ColuBroadcastTxHex.Json?, coluAccount: PrivateColuAccount?): Transaction? {
        if (txid == null) {
//            Log.e(TAG, "signTx: No transaction to sign !")
            return null
        }
        if (coluAccount == null) {
//            Log.e(TAG, "signTx: No colu account associated to transaction to sign !")
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
//            Log.e(TAG, "signTx: exception while decoding transaction hex code.")
            return null
        }

        if (txBytes == null) {
//            Log.e(TAG, "signTx: failed to decode transaction hex code.")
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
//            Log.e(TAG, "signTx: Error parsing bitcoinj transaction ! msg: " + e.message)
            return null
        }

        return signedBitlibTransaction
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val coluTx = tx as ColuTransaction
        return if (coluTx.transaction != null && coluClient.broadcastTx(coluTx.transaction!!) != null) {
            BroadcastResult(BroadcastResultType.SUCCESS)
        } else {
            BroadcastResult(BroadcastResultType.REJECTED)
        }
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

    override fun getTypicalEstimatedTransactionSize(): Int {
        // Colu transaction is a typical bitcoin transaction containing additional inputs and OP_RETURN data
        // Typical Colu transaction has 2 inputs and 4 outputs
        return FeeEstimatorBuilder().setLegacyInputs(2)
                .setLegacyOutputs(4)
                .createFeeEstimator()
                .estimateTransactionSize()
    }

}