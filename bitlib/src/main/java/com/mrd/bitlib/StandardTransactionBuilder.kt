package com.mrd.bitlib

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Function
import com.google.common.base.Preconditions
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import com.google.common.collect.Ordering
import com.mrd.bitlib.StandardTransactionBuilder.BtcOutputTooSmallException
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientBtcException
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException.BuildError
import com.mrd.bitlib.crypto.IPrivateKeyRing
import com.mrd.bitlib.crypto.IPublicKeyRing
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.InputWitness
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutputList
import com.mrd.bitlib.model.ScriptInput
import com.mrd.bitlib.model.ScriptInputP2TR
import com.mrd.bitlib.model.ScriptInputP2WPKH
import com.mrd.bitlib.model.ScriptInputP2WSH
import com.mrd.bitlib.model.ScriptInputStandard
import com.mrd.bitlib.model.ScriptOutput
import com.mrd.bitlib.model.ScriptOutputP2PKH
import com.mrd.bitlib.model.ScriptOutputP2SH
import com.mrd.bitlib.model.ScriptOutputP2TR
import com.mrd.bitlib.model.ScriptOutputP2WPKH
import com.mrd.bitlib.model.TransactionInput
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.TaprootUtils.Companion.sigHash
import org.bouncycastle.util.encoders.Hex
import java.lang.Exception
import java.lang.RuntimeException
import java.util.Collections
import java.util.LinkedList
import java.util.Locale
import java.util.Random
import javax.annotation.Nonnull

open class StandardTransactionBuilder(val network: NetworkParameters) {
    private val outputs: MutableList<TransactionOutput> = LinkedList<TransactionOutput>()

    class InsufficientBtcException(val sending: Long, val fee: Long) :
        Exception("Insufficient funds to send " + sending + " satoshis with fee " + fee) {

        companion object {
            //todo consider refactoring this into a composite return value instead of an exception. it is not really "exceptional"
            private const val serialVersionUID = 1L
        }
    }

    class BtcOutputTooSmallException(value: Long) : Exception(
        ("An output was added with a value of " + value
                + " satoshis, which is smaller than the minimum accepted by the Bitcoin network")
    ) {
        var value: Long = 0

        companion object {
            //todo consider refactoring this into a composite return value instead of an exception. it is not really "exceptional"
            private const val serialVersionUID = 1L
        }
    }

    class UnableToBuildTransactionException : Exception {
        val code: BuildError?

        constructor(msg: String?) : super(msg) {
            code = BuildError.OTHER
        }

        constructor(errorCode: BuildError?) : super("") {
            code = errorCode
        }

        enum class BuildError {
            NO_UTXO,
            PARENT_NEEDS_NO_BOOSTING,
            OTHER
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }


    @Throws(BtcOutputTooSmallException::class)
    fun addOutput(sendTo: BitcoinAddress, value: Long) {
        addOutput(createOutput(sendTo, value, network))
    }

    @Throws(BtcOutputTooSmallException::class)
    fun addOutput(output: TransactionOutput) {
        if (output.value < TransactionUtils.MINIMUM_OUTPUT_VALUE) {
            throw BtcOutputTooSmallException(output.value)
        }
        outputs.add(output)
    }

    @Throws(BtcOutputTooSmallException::class)
    fun addOutputs(outputs: OutputList) {
        for (output in outputs) {
            if (output.value > 0) {
                addOutput(output)
            }
        }
    }

    /**
     * Create an unsigned transaction and automatically calculate the miner fee.
     *
     *
     * If null is specified as the change address the 'richest' address that is part of the funding is selected as the
     * change address. This way the change always goes to the address contributing most, and the change will be less
     * than the contribution.
     *
     * @param inventory     The list of unspent transaction outputs that can be used as
     * funding
     * @param changeAddress The address to send any change to, can not be null
     * @param keyRing       The public key ring matching the unspent outputs
     * @param network       The network we are working on
     * @param minerFeeToUse The miner fee in sat to pay for every kilobytes of transaction size
     * @return An unsigned transaction or null if not enough funds were available
     */
    @Throws(InsufficientBtcException::class, UnableToBuildTransactionException::class)
    fun createUnsignedTransaction(
        inventory: Collection<UnspentTransactionOutput>,
        @Nonnull changeAddress: BitcoinAddress, keyRing: IPublicKeyRing,
        network: NetworkParameters, minerFeeToUse: Long
    ): UnsignedTransaction {
        // Make a copy so we can mutate the list
        val unspent = LinkedList<UnspentTransactionOutput>(inventory)
        val coinSelector: CoinSelector =
            FifoCoinSelector(minerFeeToUse, unspent, changeAddress.getType())
        var fee = coinSelector.getFee()
        val outputSum = coinSelector.getOutputSum()
        val funding = pruneRedundantOutputs(coinSelector.getFundings(), fee + outputSum)
        val needChangeOutputInEstimation =
            needChangeOutputInEstimation(funding, outputSum, minerFeeToUse)

        val feeEstimatorBuilder = FeeEstimatorBuilder().setArrayOfInputs(funding)
            .setArrayOfOutputs(outputs)
            .setMinerFeePerKb(minerFeeToUse)
        if (needChangeOutputInEstimation) {
            feeEstimatorBuilder.addOutput(changeAddress.getType())
        }
        fee = feeEstimatorBuilder.createFeeEstimator()
            .estimateFee()

        var found: Long = funding.sumOf { it.value }
        // We have found all the funds we need
        val toSend = fee + outputSum

        // We have our funding, calculate change
        val change = found - toSend

        // Get a copy of all outputs
        val outputs = LinkedList<TransactionOutput>(outputs)
        if (change >= TransactionUtils.MINIMUM_OUTPUT_VALUE) {
            val changeOutput = createOutput(changeAddress, change, network)
            // Select a random position for our change so it is harder to analyze our addresses in the block chain.
            // It is OK to use the weak java Random class for this purpose.
            val position = Random().nextInt(outputs.size + 1)
            outputs.add(position, changeOutput)
        }

        val unsignedTransaction = UnsignedTransaction(
            outputs,
            funding,
            keyRing,
            network,
            0,
            UnsignedTransaction.NO_SEQUENCE
        )

        // check if we have a reasonable Fee or throw an error otherwise
        val estimator = FeeEstimatorBuilder().setArrayOfInputs(unsignedTransaction.fundingOutputs)
            .setArrayOfOutputs(unsignedTransaction.outputs)
            .createFeeEstimator()
        val estimateTransactionSize = estimator.estimateTransactionSize()
        val calculatedFee = unsignedTransaction.calculateFee()
        val estimatedFeePerKb =
            (calculatedFee.toFloat() / (estimateTransactionSize.toFloat() / 1000)).toLong()
                .toFloat()

        // set a limit of MAX_MINER_FEE_PER_KB as absolute limit - it is very likely a bug in the fee estimator or transaction composer
        if (estimatedFeePerKb > BitcoinTransaction.MAX_MINER_FEE_PER_KB) {
            throw UnableToBuildTransactionException(
                String.format(
                    Locale.getDefault(),
                    "Unreasonable high transaction fee of %s sat/1000Byte on a %d Bytes tx. Fee: %d sat, Suggested fee: %d sat",
                    estimatedFeePerKb, estimateTransactionSize, calculatedFee, minerFeeToUse
                )
            )
        }

        return unsignedTransaction
    }

    private fun needChangeOutputInEstimation(
        funding: List<UnspentTransactionOutput>,
        outputSum: Long, minerFeeToUse: Long
    ): Boolean {
        val feeEstimator = FeeEstimatorBuilder().setArrayOfInputs(funding)
            .setArrayOfOutputs(outputs)
            .setMinerFeePerKb(minerFeeToUse)
            .createFeeEstimator()
        val fee = feeEstimator.estimateFee()

        var found: Long = funding.sumOf { it.value }
        // We have found all the funds we need
        val toSend = fee + outputSum

        // We have our funding, calculate change
        val change = found - toSend

        return if (change >= TransactionUtils.MINIMUM_OUTPUT_VALUE) {
            // We need to add a change output in the estimation.
            true
        } else {
            // The change output would be smaller (or zero) than what the network would accept.
            // In this case we leave it be as a small increased miner fee.
            false
        }
    }


    /**
     * Greedy picks the biggest UTXOs until the outputSum is met.
     * @param funding UTXO list in any order
     * @param outputSum amount to spend
     * @return shuffled list of UTXOs
     */
    private fun pruneRedundantOutputs(
        funding: List<UnspentTransactionOutput>,
        outputSum: Long
    ): List<UnspentTransactionOutput> {
        val largestToSmallest = Ordering.natural<Comparable<*>?>().reverse<Comparable<*>?>()
            .onResultOf<UnspentTransactionOutput?>(object :
                Function<UnspentTransactionOutput, Comparable<*>?> {
                override fun apply(input: UnspentTransactionOutput?): Comparable<*>? = input?.value
            }).sortedCopy<UnspentTransactionOutput?>(funding)

        var target: Long = 0
        largestToSmallest.forEachIndexed { i, output ->
            target += output.value
            if (target >= outputSum) {
                val ret = largestToSmallest.subList(0, i + 1)
                ret.shuffle()
                return ret
            }
        }
        return largestToSmallest
    }

    @VisibleForTesting
    fun getRichest(
        unspent: Collection<UnspentTransactionOutput?>,
        network: NetworkParameters?
    ): BitcoinAddress {
        Preconditions.checkArgument(!unspent.isEmpty())
        val txout2Address: Function<UnspentTransactionOutput?, BitcoinAddress?> =
            object : Function<UnspentTransactionOutput?, BitcoinAddress?> {
                override fun apply(input: UnspentTransactionOutput?): BitcoinAddress? =
                    input?.script?.getAddress(network)
            }
        val index =
            Multimaps.index<BitcoinAddress, UnspentTransactionOutput>(unspent, txout2Address)
        val ret = getRichest(index)
        return Preconditions.checkNotNull<BitcoinAddress>(ret)
    }

    private fun getRichest(index: Multimap<BitcoinAddress, UnspentTransactionOutput>): BitcoinAddress? {
        var ret: BitcoinAddress? = null
        var maxSum: Long = 0
        index.keys().forEach { address ->
            val unspentTransactionOutputs = index.get(address)
            val newSum = sum(unspentTransactionOutputs)
            if (newSum > maxSum) {
                ret = address
                maxSum = newSum
            }
        }
        return ret
    }

    private fun sum(outputs: Iterable<UnspentTransactionOutput>): Long {
        var sum: Long = 0
        for (output in outputs) {
            sum += output.value
        }
        return sum
    }

    private fun outputSum(): Long = outputs.sumOf { it.value }

    private interface CoinSelector {
        fun getFundings(): List<UnspentTransactionOutput>
        fun getFee(): Long
        fun getOutputSum(): Long
    }

    private inner class FifoCoinSelector(
        feeSatPerKb: Long,
        unspent: MutableList<UnspentTransactionOutput>,
        changeType: AddressType
    ) : CoinSelector {
        private val allFunding: MutableList<UnspentTransactionOutput>
        private var feeSat: Long
        private val outputSum: Long

        init {
            // Find the funding for this transaction
            allFunding = LinkedList<UnspentTransactionOutput>()
            val feeEstimatorBuilder = FeeEstimatorBuilder().setArrayOfInputs(unspent)
                .addOutput(changeType)
                .setMinerFeePerKb(feeSatPerKb)
            val feeEstimator = feeEstimatorBuilder
                .createFeeEstimator()
            feeSat = feeEstimator.estimateFee()
            outputSum = outputSum()
            var foundSat: Long = 0
            while (foundSat < feeSat + outputSum) {
                val unspentTransactionOutput = extractOldest(unspent)
                if (unspentTransactionOutput == null) {
                    // We do not have enough funds
                    throw InsufficientBtcException(outputSum, feeSat)
                }
                foundSat += unspentTransactionOutput.value
                allFunding.add(unspentTransactionOutput)

                val estimatorBuilder = feeEstimatorBuilder.setArrayOfInputs(allFunding)
                    .setArrayOfOutputs(outputs)
                    .setMinerFeePerKb(feeSatPerKb)
                if (needChangeOutputInEstimation(allFunding, outputSum, feeSatPerKb)) {
                    estimatorBuilder.addOutput(changeType)
                }
                feeSat = estimatorBuilder.createFeeEstimator().estimateFee()
            }
        }

        override fun getFundings(): List<UnspentTransactionOutput> = allFunding


        override fun getFee(): Long = feeSat

        override fun getOutputSum(): Long = outputSum

        fun extractOldest(unspent: MutableCollection<UnspentTransactionOutput>): UnspentTransactionOutput? {
            // find the "oldest" output
            var minHeight = Int.Companion.MAX_VALUE
            var oldest: UnspentTransactionOutput? = null
            for (output in unspent) {
                if (output.script !is ScriptOutputP2PKH && output.script !is ScriptOutputP2SH && output.script !is ScriptOutputP2WPKH && output.script !is ScriptOutputP2TR) {
                    // only look for certain scripts
                    continue
                }

                // Unconfirmed outputs have height = -1 -> change this to Int.MAX-1, so that we
                // choose them as the last possible option
                val height = if (output.height > 0) output.height else Int.Companion.MAX_VALUE - 1

                if (height < minHeight) {
                    minHeight = height
                    oldest = output
                }
            }
            if (oldest == null) {
                // There were no outputs
                return null
            }
            unspent.remove(oldest)
            return oldest
        }
    }

    companion object {
        // hash size 32 + output index size 4 + script length 1 + max. script size for compressed keys 107 + sequence number 4
        // also see https://github.com/bitcoin/bitcoin/blob/master/src/primitives/transaction.h#L190
        val MAX_INPUT_SIZE: Int = 32 + 4 + 1 + 107 + 4
        fun createOutput(
            sendTo: BitcoinAddress,
            value: Long,
            network: NetworkParameters
        ): TransactionOutput {
            var script: ScriptOutput?
            when (sendTo.getType()) {
                AddressType.P2SH_P2WPKH -> script = ScriptOutputP2SH(sendTo.getTypeSpecificBytes())
                AddressType.P2PKH -> script = ScriptOutputP2PKH(sendTo.getTypeSpecificBytes())
                AddressType.P2WPKH -> script = ScriptOutputP2WPKH(sendTo.getTypeSpecificBytes())
                AddressType.P2TR -> script = ScriptOutputP2TR(sendTo.getTypeSpecificBytes())
                else -> throw NotImplementedError()
            }
            return TransactionOutput(value, script)
        }

        fun generateSignatures(
            requests: Array<SigningRequest>,
            keyRing: IPrivateKeyRing
        ): List<ByteArray> {
            val signatures = LinkedList<ByteArray>()
            requests.forEach { request ->
                val signer = keyRing.findSignerByPublicKey(request.publicKey)
                if (signer == null) {
                    // This should not happen as we only work on outputs that we have
                    // keys for
                    throw RuntimeException("Private key not found")
                }
                if (request.signAlgo == SignAlgorithm.Standard) {
                    val signature = signer.makeStandardBitcoinSignature(request.toSign)
                    signatures.add(signature)
                } else if (request.signAlgo == SignAlgorithm.Schnorr) {
                    val sigHash = sigHash(request.message)
                    val signature = signer.makeSchnorrBitcoinSignature(sigHash, ByteArray(0))
                    signatures.add(signature)
                }
            }
            return signatures
        }

        @JvmStatic
        fun finalizeTransaction(
            unsigned: UnsignedTransaction,
            signatures: List<ByteArray>
        ): BitcoinTransaction {
            // Create finalized transaction inputs
            val funding = unsigned.fundingOutputs
            val inputs = arrayOfNulls<TransactionInput>(funding.size)
            var version = 1
            for (i in funding.indices) {
                if (isScriptInputSegWit(unsigned, i)) {
                    inputs[i] = unsigned.inputs[i]
                    if (inputs[i]!!.script is ScriptInputP2WPKH && !(inputs[i]!!.script as ScriptInputP2WPKH).isNested) {
                        inputs[i]!!.script = ScriptInput.EMPTY
                    }
                    val witness = InputWitness(2)
                    witness.setStack(0, signatures.get(i))
                    witness.setStack(1, unsigned.signingRequests[i]!!.publicKey.publicKeyBytes)
                    inputs[i]!!.witness = witness
                } else if (isScriptInputP2TR(unsigned, i)) {
                    inputs[i] = TransactionInput(funding[i].outPoint, ScriptInput.EMPTY)
                    val witness = InputWitness(1)
                    witness.setStack(0, signatures[i] + HexUtils.toBytes("01"))
                    inputs[i]!!.witness = witness
                    version = 2
                } else {
                    // Create script from signature and public key
                    val script = ScriptInputStandard(
                        signatures.get(i),
                        unsigned.signingRequests[i]!!.publicKey.publicKeyBytes
                    )
                    inputs[i] = TransactionInput(
                        funding[i].outPoint,
                        script,
                        unsigned.defaultSequenceNumber,
                        funding[i].value
                    )
                }
            }

            // Create transaction with valid outputs and empty inputs
            return BitcoinTransaction(version, inputs, unsigned.outputs, unsigned.lockTime)
        }

        private fun isScriptInputSegWit(unsigned: UnsignedTransaction, i: Int): Boolean =
            unsigned.inputs[i].script is ScriptInputP2WPKH || unsigned.inputs[i].script is ScriptInputP2WSH

        private fun isScriptInputP2TR(unsigned: UnsignedTransaction, i: Int): Boolean =
            unsigned.inputs[i].script is ScriptInputP2TR
    }
}
