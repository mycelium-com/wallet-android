package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericFee
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionDecoder
import org.web3j.crypto.TransactionEncoder
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class EthTransaction(val type: CryptoCurrency, val toAddress: GenericAddress, val value: Value, val gasPrice: GenericFee,
                     @Transient var rawTransaction: RawTransaction) : GenericTransaction(type) {
    var signedHex: String? = null
    var txHash: ByteArray? = null
    override fun getId() = txHash

    override fun txBytes() = TransactionEncoder.encode(rawTransaction)!!

    override fun getEstimatedTransactionSize() = 21000

    /**
     * Always treat de-serialization as a full-blown constructor, by
     * validating the final state of the de-serialized object.
     */
    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(inputStream: ObjectInputStream) {
        //always perform the default de-serialization first
        inputStream.defaultReadObject()


        val transactionBytes = inputStream.readUTF()
        rawTransaction = TransactionDecoder.decode(transactionBytes)
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        out.writeUTF(HexUtils.toHex(this.txBytes()))
    }
}