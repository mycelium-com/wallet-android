package com.mrd.bitlib.model

import com.google.common.primitives.UnsignedInteger
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.BitcoinTransaction.TransactionParsingException
import com.mrd.bitlib.model.Script.ScriptParsingException
import com.mrd.bitlib.model.TransactionInput.TransactionInputParsingException
import com.mrd.bitlib.model.TransactionOutput.TransactionOutputParsingException
import com.mrd.bitlib.model.signature.TaprootCommonSignatureMessageBuilder
import com.mrd.bitlib.model.signature.WitnessSignatureMessageBuilder
import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.Sha256Hash
import java.io.Serializable
import java.lang.Error
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.RuntimeException

/**
 * Transaction represents a raw Bitcoin transaction. In other words, it contains only the information found in the
 * byte string representing a Bitcoin transaction. It contains no contextual information, such as the height
 * of the transaction in the block chain or the outputs that its inputs redeem.
 *
 *
 * Implements Serializable and is inserted directly in and out of the database. Therefore it cannot be changed
 * without messing with the database.
 */
class BitcoinTransaction(
    val version: Int,
    @JvmField
    val inputs: Array<TransactionInput>,
    @JvmField
    val outputs: Array<TransactionOutput>,
    val lockTime: Int,
    @Transient
    var txSize: Int = -1,
    // we already know the hash of this transaction, dont recompute it
    _hash: Sha256Hash? = null,
    _id: Sha256Hash? = null
) : Serializable {

    val id: Sha256Hash by lazy {
        if (_id == null)
            HashUtils.doubleSha256(ByteWriter(2000).apply {
                toByteWriter(this, false)
            }.toBytes()).reverse()
        else _id
    }

    val hash: Sha256Hash by lazy {
        if (_hash == null)
            HashUtils.doubleSha256(ByteWriter(2000).apply { toByteWriter(this) }.toBytes())
                .reverse()
        else _hash
    }

    //    private Sha256Hash _unmalleableHash;
    // cache for some getters that need to do some work and might get called often
    @Transient
    private var _rbfAble: Boolean? = null

    constructor(copyFrom: BitcoinTransaction) : this(
        copyFrom.version, copyFrom.inputs, copyFrom.outputs, copyFrom.lockTime, copyFrom.txSize,
        copyFrom.hash, copyFrom.id
    )

    fun copy(): BitcoinTransaction =
        try {
            fromByteReader(ByteReader(toBytes()))
        } catch (e: TransactionParsingException) {
            // This should never happen
            throw RuntimeException(e)
        }

    @JvmOverloads
    fun toBytes(asSegwit: Boolean = true): ByteArray? =
        ByteWriter(1024).apply { toByteWriter(this, asSegwit) }.toBytes()

    val txRawSize: Int
        get() {
            if (txSize == -1) {
                txSize = toBytes()!!.size
            }
            return txSize
        }

    /**
     * Same as [.toByteWriter], but allows to enforce SegWit tx serialization to classic format.
     *
     * @param asSegwit if true tx would be serialized according bip144 standard.
     */
    /**
     * This method serializes transaction according to [BIP144](https://github.com/bitcoin/bips/blob/master/bip-0144.mediawiki)
     */
    @JvmOverloads
    fun toByteWriter(writer: ByteWriter, asSegwit: Boolean = true) {
        writer.putIntLE(version)
        val isSegwit = isSegwit()
        val isSegWitMode = asSegwit && (isSegwit)
        var segwitPart = 0
        if (isSegWitMode) {
            segwitPart -= writer.length()
            writer.putCompactInt(0) //marker
            writer.putCompactInt(1) //flag
            segwitPart += writer.length()
        }
        writeInputs(writer)
        writeOutputs(writer)
        if (isSegWitMode) {
            segwitPart -= writer.length()
            writeWitness(writer)
            segwitPart += writer.length()
        }
        writer.putIntLE(lockTime)
        vSizeTotal = writer.length()
        if (asSegwit) {
            if (isSegwit) {
                vSizeBase = vSizeTotal - segwitPart
            } else {
                vSizeBase = writer.length()
            }
        }
    }

    // cash size calculation for speed up working with txs
    private var vSizeBase = 0
    private var vSizeTotal = 0

    fun vsize(): Int {
        if (vSizeBase == 0) {
            toBytes(true)
        }
        // vsize calculations are from https://github.com/bitcoin/bips/blob/master/bip-0141.mediawiki#transaction-size-calculations
        // ... + 3 ) / 4 deals with the int cast rounding down but us needing to round up.
        return (vSizeBase * 3 + vSizeTotal + 3) / 4
    }

    private fun writeWitness(writer: ByteWriter) {
        inputs.forEach { input ->
            input.witness.toByteWriter(writer)
        }
    }

    private fun writeInputs(writer: ByteWriter) {
        writer.putCompactInt(inputs.size.toLong())
        inputs.forEach { input ->
            input.toByteWriter(writer)
        }
    }

    private fun writeOutputs(writer: ByteWriter) {
        writer.putCompactInt(outputs.size.toLong())
        outputs.forEach { output ->
            output.toByteWriter(writer)
        }
    }

    fun getTxDigestHash(i: Int): Sha256Hash =
        HashUtils.doubleSha256(ByteWriter(1024).apply {
            if (inputs[i].script is ScriptInputP2WSH || inputs[i].script is ScriptInputP2WPKH) {
                WitnessSignatureMessageBuilder(this@BitcoinTransaction, i, version).build(this)
            } else if (inputs[i].script is ScriptInputP2TR) {
                throw RuntimeException("T2TR use commonSignatureMessage")
            } else {
                toByteWriter(this, false)
            }
            // We also have to write a hash type.
            val hashType = 1
            putIntLE(hashType)
        }.toBytes())

    fun commonSignatureMessage(i: Int, utxos: Array<UnspentTransactionOutput>): ByteArray =
        if (inputs[i].script is ScriptInputP2TR) {
            ByteWriter(1024).apply {
                TaprootCommonSignatureMessageBuilder(this@BitcoinTransaction, utxos, i, version).build(this)
            }.toBytes()
        } else {
            throw RuntimeException("commonSignatureMessage only for P2TR, vin $i")
        }

    /**
     * Returns the minimum nSequence number of all inputs
     * Can be used to detect transactions marked for Full-RBF and thus are very low trust while having 0 conf
     * Transactions with minSequenceNumber < MAX_INT-1 are eligible for full RBF
     * https://github.com/bitcoin/bitcoin/pull/6871#event-476297575
     *
     * @return the min nSequence of all inputs of that transaction
     */
    fun getMinSequenceNumber(): UnsignedInteger =
        inputs.minOf { UnsignedInteger.fromIntBits(it.sequence) } ?: UnsignedInteger.MAX_VALUE

    /**
     * Returns true if this transaction is marked for RBF and thus can easily get replaced by a
     * conflicting transaction while it is still unconfirmed.
     *
     * @return true if any of its inputs has a nSequence < MAX_INT-1
     */
    fun isRbfAble(): Boolean {
        if (_rbfAble == null) {
            _rbfAble =
                (getMinSequenceNumber() < UnsignedInteger.MAX_VALUE.minus(UnsignedInteger.ONE))
        }
        return _rbfAble!!
    }

    /**
     * @return true if transaction is SegWit, else false
     */
    fun isSegwit(): Boolean =
        inputs.any { it.hasWitness() }

    override fun toString(): String =
        id.toString() + " in: " + inputs.size + " out: " + outputs.size

    override fun hashCode(): Int = hash.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is BitcoinTransaction) {
            return false
        }
        return hash == other.hash
    }

    val isCoinbase: Boolean
        get() = inputs.any { it.script is ScriptInputCoinbase }

    class TransactionParsingException : Exception {
        constructor(message: String?) : super(message)

        constructor(message: String?, e: Exception?) : super(message, e)

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
        private const val ONE_uBTC_IN_SATOSHIS: Long = 100
        private val ONE_mBTC_IN_SATOSHIS: Long = 1000 * ONE_uBTC_IN_SATOSHIS

        @JvmField
        val MAX_MINER_FEE_PER_KB: Long = 200L * ONE_mBTC_IN_SATOSHIS // 20000sat/B

        @JvmStatic
        fun fromUnsignedTransaction(unsignedTransaction: UnsignedTransaction): BitcoinTransaction {
            val inputs = unsignedTransaction.fundingOutputs.mapIndexed { idx, fundingOutput ->
                val script = if (unsignedTransaction.isSegWitOutput(idx)) {
                    val segWitScriptBytes = unsignedTransaction.inputs[idx].script.scriptBytes
                    try {
                        ScriptInput.fromScriptBytes(segWitScriptBytes)
                    } catch (e: ScriptParsingException) {
                        //Should never happen
                        throw Error("Parsing segWitScriptBytes failed")
                    }
                } else {
                    ScriptInput(fundingOutput.script.getScriptBytes())
                }
                TransactionInput(
                    fundingOutput.outPoint,
                    script,
                    unsignedTransaction.defaultSequenceNumber,
                    fundingOutput.value
                )
            }
            return BitcoinTransaction(
                1,
                inputs.toTypedArray(),
                unsignedTransaction.outputs,
                unsignedTransaction.lockTime
            )
        }

        @JvmStatic
        @Throws(TransactionParsingException::class)
        fun fromBytes(transaction: ByteArray?): BitcoinTransaction =
            fromByteReader(ByteReader(transaction))

        // use this builder if you already know the resulting transaction hash to speed up computation
        @JvmStatic
        @JvmOverloads
        @Throws(TransactionParsingException::class)
        fun fromByteReader(
            reader: ByteReader,
            knownTransactionHash: Sha256Hash? = null
        ): BitcoinTransaction {
            val size = reader.available()
            try {
                val version = reader.getIntLE()
                var useSegwit = false
                val marker = peekByte(reader)
                if (marker.toInt() == 0) {
                    //segwit possible
                    reader.get()
                    val flag: Byte = peekByte(reader)
                    if (flag.toInt() == 1) {
                        //it's segwit
                        reader.get()
                        useSegwit = true
                    } else {
                        throw TransactionParsingException("Unable to parse segwit transaction. Flag must be 0x01")
                    }
                }

                val inputs = parseTransactionInputs(reader)
                val outputs = parseTransactionOutputs(reader)

                if (useSegwit) {
                    parseWitness(reader, inputs)
                }

                val lockTime = reader.getIntLE()
                return BitcoinTransaction(
                    version,
                    inputs,
                    outputs,
                    lockTime,
                    size,
                    knownTransactionHash
                )
            } catch (e: InsufficientBytesException) {
                throw TransactionParsingException(e.message)
            }
        }

        @Throws(InsufficientBytesException::class)
        private fun parseWitness(reader: ByteReader, inputs: Array<TransactionInput>) {
            inputs.forEach { input ->
                val stackSize = reader.compactInt.toInt()
                input.witness = InputWitness(stackSize).apply {
                    (0..stackSize - 1).map { y ->
                        val pushSize = reader.compactInt
                        val push = reader.getBytes(pushSize.toInt())
                        setStack(y, push)
                    }
                }
            }
        }

        @Throws(InsufficientBytesException::class, TransactionParsingException::class)
        private fun parseTransactionOutputs(reader: ByteReader): Array<TransactionOutput> =
            (0..reader.compactInt.toInt() - 1).map { i ->
                try {
                    TransactionOutput.fromByteReader(reader)
                } catch (e: TransactionOutputParsingException) {
                    throw TransactionParsingException(
                        ("Unable to parse transaction output at index " + i + ": "
                                + e.message)
                    )
                }
            }.toTypedArray()

        @Throws(InsufficientBytesException::class, TransactionParsingException::class)
        private fun parseTransactionInputs(reader: ByteReader): Array<TransactionInput> =
            (0..reader.compactInt.toInt() - 1).map { i ->
                try {
                    TransactionInput.fromByteReader(reader)
                } catch (e: TransactionInputParsingException) {
                    throw TransactionParsingException(
                        ("Unable to parse transaction input at index " + i + ": "
                                + e.message), e
                    )
                } catch (e: IllegalStateException) {
                    throw TransactionParsingException(
                        ("ISE - Unable to parse transaction input at index " + i + ": "
                                + e.message), e
                    )
                }
            }.toTypedArray()

        @Throws(InsufficientBytesException::class)
        private fun peekByte(reader: ByteReader): Byte {
            val b = reader.get()
            reader.position = reader.position - 1
            return b
        }
    }
}
