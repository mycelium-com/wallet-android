package com.mrd.bitlib.model

import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import java.io.Serializable
import java.lang.Exception

/**
 * OutPoint is a reference to a particular 0-based output index of a given transaction identified by its txid.
 *
 * A hash of all 0s is spent from in Coinbase Transactions.
 */
class OutPoint(
    @JvmField
    val txid: Sha256Hash,
    @JvmField
    var index: Int) : Serializable {

    constructor(reader: ByteReader) : this(reader.getSha256Hash(), reader.compactInt.toInt())

    override fun hashCode(): Int = txid.hashCode() + index

    override fun equals(other: Any?): Boolean {
        if (other !is OutPoint) {
            return false
        }
        return txid == other.txid && index == other.index
    }

    override fun toString(): String = "$txid:$index"

    fun toByteWriter(writer: ByteWriter): ByteWriter {
        writer.putSha256Hash(txid)
        writer.putCompactInt(index.toLong())
        return writer
    }

    /**
     * Required for BIP143 tx digest.
     */
    fun hashPrev(writer: ByteWriter) {
        writer.putSha256Hash(txid, true)
        writer.putIntLE(index)
    }

    companion object {
        private const val serialVersionUID = 1L

        // A coinbase transaction spends from the hash 00000... (not the txid(00000...) ). COINBASE_OUTPOINT is just that: 000000...:0
        // Or is it 0000...:Integer.MAX_VALUE?
        // So far, the index of the COINBASE_OUTPOINT isn't being used.
        val COINBASE_OUTPOINT: OutPoint = OutPoint(Sha256Hash.ZERO_HASH, -0x1)

        @JvmStatic
        fun fromString(string: String?): OutPoint? {
            try {
                if (string == null) {
                    return null
                }
                val colon = string.indexOf(':')
                if (colon == -1) {
                    return null
                }
                val txid = string.substring(0, colon)
                if (txid.length != 64) {
                    return null
                }
                val bytes = HexUtils.toBytes(txid)
                if (bytes == null) {
                    return null
                }
                val indexString = string.substring(colon + 1)
                val index = indexString.toInt()
                return OutPoint(Sha256Hash(bytes), index)
            } catch (e: Exception) {
                return null
            }
        }
    }
}
