package com.mycelium.wapi.wallet.eth

import org.junit.Assert.fail
import org.junit.Test
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger

class EthSendTxTest {
    // use this mnemonic to have 0xD7677B6e62F283E1775B05d9e875B03C27c298a9 as first ethereum account:
    // "else tape female vast twist mandate lucky now license stand skull garment"

    // 0x021D61c16ed105e491210360a49bb793d5eB85b0
    private val credentials1: Credentials = Credentials.create("0xBD7AAA21DE06DA4E982FF51AFAD6E6E654CA456F212A0A5D235ACF6707EF8C9F")
    // 0xD7677B6e62F283E1775B05d9e875B03C27c298a9
    private val credentials2: Credentials = Credentials.create("0x64B248B0A9D17EEC1D7F53A441E96EA9BD755C82A6AD7C95EF6F5D90EDC009F6")
    private val web3j: Web3j = Web3j.build(HttpService("http://ropsten-index.mycelium.com:18545"))
    private val value = Convert.toWei("0.0001", Convert.Unit.ETHER).toBigInteger()

    @Test
    fun account1_account2() {
        val howMuchToSend = 100
        var nonce = getNonce(credentials1.address)
        for (i in 1..howMuchToSend) {
            sendTx12(nonce++)
        }
    }

    @Test
    fun account2_account1() {
        val howMuchToSend = 100
        var nonce = getNonce(credentials2.address)
        for (i in 1..howMuchToSend) {
            sendTx21(nonce++)
        }
    }

    private fun sendTx12(nonce: BigInteger) {
        try {
            val rawTransaction =
                    RawTransaction.createEtherTransaction(nonce, Convert.toWei("1", Convert.Unit.GWEI).toBigInteger(), BigInteger.valueOf(21000), credentials2.address, value)
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials1)
            val hexValue = Numeric.toHexString(signedMessage)
            val result = web3j.ethSendRawTransaction(hexValue).sendAsync().get()
            if (result.hasError()) {
                throw Exception(result.error.message)
            }
        } catch (e: Exception) {
            fail(e.localizedMessage)
        }
    }

    private fun sendTx21(nonce: BigInteger) {
        try {
            val rawTransaction =
                    RawTransaction.createEtherTransaction(nonce, Convert.toWei("1", Convert.Unit.GWEI).toBigInteger(), BigInteger.valueOf(21000), credentials1.address, value)
            val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials2)
            val hexValue = Numeric.toHexString(signedMessage)
            val result = web3j.ethSendRawTransaction(hexValue).sendAsync().get()
            if (result.hasError()) {
                throw Exception(result.error.message)
            }
        } catch (e: Exception) {
            fail(e.localizedMessage)
        }
    }

    private fun getNonce(address: String): BigInteger {
        val ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get()

        return ethGetTransactionCount.transactionCount
    }
}