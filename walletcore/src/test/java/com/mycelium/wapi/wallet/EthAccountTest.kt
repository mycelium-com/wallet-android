package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.eth.coins.EthTest
import junit.framework.Assert.assertNotNull
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthSendTransaction
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger


class EthAccountTest {
    private val web3j: Web3j = Web3j.build(InfuraHttpService("https://ropsten.infura.io/WKXR51My1g5Ea8Z5Xh3l"))
    private val credentials: Credentials = Credentials.create("0xBD7AAA21DE06DA4E982FF51AFAD6E6E654CA456F212A0A5D235ACF6707EF8C9F")
    private val coinType = EthTest


    private fun createTx(toAddress: GenericAddress, value: Value, gasPrice: FeePerKbFee): GenericTransaction {
        val nonce = getNonce(credentials.address)

        val rawTransaction =
                RawTransaction.createEtherTransaction(nonce, BigInteger.valueOf(gasPrice.feePerKb.value), BigInteger.valueOf(21000), toAddress.toString(), BigInteger.valueOf(value.value))
        val ethTransaction = EthTransaction(coinType, toAddress, value, gasPrice)
        ethTransaction.rawTransaction = rawTransaction
        return ethTransaction
    }

    @Throws(Exception::class)
    fun getNonce(address: String): BigInteger {
        val ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get()

        return ethGetTransactionCount.transactionCount
    }

    private fun signTx(signRequest: GenericTransaction) {
        val rawTransaction = (signRequest as EthTransaction).rawTransaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        signRequest.signedHex = hexValue
    }

    private fun broadcastTx(signedTx: GenericTransaction) {
        val ethSendTransaction = web3j.ethSendRawTransaction((signedTx as EthTransaction).signedHex).sendAsync().get()

        signedTx.ethSendTransaction = ethSendTransaction
    }

    @Test
    fun onlineSigning() {

//        val credentials = Credentials.create(privkey)
//        try {
//            val transactionReceipt = sendFunds(
//                    web3j, credentials, "0xd7677b6e62f283e1775b05d9e875b03c27c298a9",
//                    BigDecimal.valueOf(0.00015), Convert.Unit.ETHER).send()
//            transactionReceipt
//
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

    }

    @Test
    fun onfflineSigning() {
        val recepient = EthAddress(coinType, "0xD7677B6e62F283E1775B05d9e875B03C27c298a9")
        val value = Value.valueOf(coinType, Convert.toWei("0.0001", Convert.Unit.ETHER).toBigInteger())
        val gasPrice = FeePerKbFee(Value.valueOf(coinType, Convert.toWei("20", Convert.Unit.GWEI).toBigInteger()))
        try {
            val ethTransaction = createTx(recepient, value, gasPrice)
            signTx(ethTransaction)
            broadcastTx(ethTransaction)
            assertNotNull((ethTransaction as EthTransaction).ethSendTransaction?.transactionHash)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class EthTransaction(val type: CryptoCurrency, val toAddress: GenericAddress, val value: Value, val gasPrice: GenericFee) : GenericTransaction(type) {
    var rawTransaction: RawTransaction? = null
    var signedHex: String? = null
    var ethSendTransaction: EthSendTransaction? = null
    override fun getId(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun txBytes(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getEstimatedTransactionSize() = 21000
}