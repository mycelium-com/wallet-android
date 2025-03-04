package com.mrd.bitlib

import com.google.common.collect.ImmutableList
import com.megiontechnologies.Bitcoins
import com.mrd.bitlib.crypto.BitcoinSigner
import com.mrd.bitlib.crypto.IPrivateKeyRing
import com.mrd.bitlib.crypto.IPublicKeyRing
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutPoint
import com.mrd.bitlib.model.ScriptOutputP2PKH
import com.mrd.bitlib.model.TransactionOutput
import com.mrd.bitlib.model.UnspentTransactionOutput
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.ArrayList
import kotlin.math.pow

class StandardTransactionBuilderTest {
    private var testme: StandardTransactionBuilder? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        testme = StandardTransactionBuilder(network)
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testEmptyList() {
        testRichest(ImmutableList.of<UnspentTransactionOutput?>(), ADDRS[0])
    }

    @Test
    @Throws(Exception::class)
    fun testSingleList() {
        for (i in 0 until COUNT) {
            testRichest(ImmutableList.of<UnspentTransactionOutput?>(UTXOS[i][0]), ADDRS[i])
        }
    }

    @Test
    @Throws(Exception::class)
    fun testList() {
        for (i in 1 until COUNT) {
            val utxos: MutableList<UnspentTransactionOutput?> =
                ArrayList<UnspentTransactionOutput?>(2 * i + 1)
            for (j in 0 until i) {
                utxos.add(UTXOS[j][0])
                utxos.add(UTXOS[j][1])
            }
            utxos.add(UTXOS[i][0])
            testRichest(utxos, ADDRS[i - 1])
            utxos.reverse()
            testRichest(utxos, ADDRS[i - 1])
            utxos.add(UTXOS[i][1])
            testRichest(utxos, ADDRS[i])
            utxos.reverse()
            testRichest(utxos, ADDRS[i])
        }
    }

    private fun testRichest(utxos: Collection<UnspentTransactionOutput?>, winner: BitcoinAddress?) {
        val address = testme!!.getRichest(utxos, network)
        Assert.assertEquals(winner, address)
    }

    @Test
    @Throws(Exception::class)
    fun testCreateUnsignedTransactionWithoutChange() {
        val feeExpected = FeeEstimatorBuilder().setLegacyInputs(1)
            .setLegacyOutputs(1)
            .setMinerFeePerKb(200000)
            .createFeeEstimator()
            .estimateFee().toInt()

        println(feeExpected) //38400
        val utxoAvailable =
            2 * Bitcoins.SATOSHIS_PER_BITCOIN + feeExpected + TransactionUtils.MINIMUM_OUTPUT_VALUE - 10
        // UTXOs worth utxoAvailable satoshis, should result in 1 in 1 out.
        // MINIMUM_OUTPUT_VALUE - 10 satoshis will be
        // left out because it is inferior of the MINIMUM_OUTPUT_VALUE.
        val inventory = ImmutableList.of<UnspentTransactionOutput?>(
            getUtxo(ADDRS[0]!!, utxoAvailable)
        )
        testme!!.addOutput(ADDRS[2]!!, 2 * Bitcoins.SATOSHIS_PER_BITCOIN)
        val tx = testme!!.createUnsignedTransaction(
            inventory, ADDRS[3]!!, KEY_RING,
            network, 200000
        )
        val inputs: Array<UnspentTransactionOutput> = tx.fundingOutputs
        Assert.assertEquals(1, inputs.size.toLong())
        Assert.assertEquals(utxoAvailable, inputs[0].value)

        val outputs: Array<TransactionOutput> = tx.outputs
        Assert.assertEquals(1, outputs.size.toLong())
        Assert.assertTrue(tx.calculateFee() < feeExpected + TransactionUtils.MINIMUM_OUTPUT_VALUE)
        Assert.assertTrue(tx.calculateFee() > feeExpected)
        Assert.assertEquals(ADDRS[2], outputs[0].script.getAddress(network))
    }

    @Test
    @Throws(Exception::class)
    fun testCreateUnsignedTransactionWithChange() {
        // UTXOs worth 10BTC, spending 1BTC should result in 1 in 2 out, spending 1 and 9-fee
        val inventory = ImmutableList.of<UnspentTransactionOutput?>(
            getUtxo(ADDRS[0]!!, 10 * Bitcoins.SATOSHIS_PER_BITCOIN)
        )
        testme!!.addOutput(ADDRS[1]!!, Bitcoins.SATOSHIS_PER_BITCOIN)
        val feeExpected = FeeEstimatorBuilder().setLegacyInputs(1)
            .setLegacyOutputs(2)
            .setMinerFeePerKb(200000)
            .createFeeEstimator()
            .estimateFee().toInt()

        val tx = testme!!.createUnsignedTransaction(
            inventory, ADDRS[2]!!, KEY_RING,
            network, 200000
        ) // miner fees to use = 200 satoshis per bytes.
        val inputs: Array<UnspentTransactionOutput> = tx.fundingOutputs
        Assert.assertEquals(1, inputs.size.toLong())
        val outputs: Array<TransactionOutput> = tx.outputs
        Assert.assertEquals(2, outputs.size.toLong())
        Assert.assertEquals(feeExpected.toLong(), tx.calculateFee())
        Assert.assertEquals(
            10 * Bitcoins.SATOSHIS_PER_BITCOIN,
            outputs[0].value + outputs[1].value + tx.calculateFee()
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCreateUnsignedTransactionMinToFee() {
        // UTXOs worth 2MIN + 1 + 3, spending MIN should result in just one output
        val inventory = listOf<UnspentTransactionOutput>(UTXOS[0][0]!!, UTXOS[0][1]!!)
        testme!!.addOutput(ADDRS[1]!!, TransactionUtils.MINIMUM_OUTPUT_VALUE)
        val tx =
            testme!!.createUnsignedTransaction(inventory, ADDRS[2]!!, KEY_RING, network, 1000)
        val inputs: Array<UnspentTransactionOutput> = tx.fundingOutputs
        Assert.assertEquals(2, inputs.size.toLong())
        val outputs: Array<TransactionOutput> = tx.outputs
        Assert.assertEquals(1, outputs.size.toLong())
        Assert.assertEquals(ADDRS[1], outputs[0].script.getAddress(network))
    }


    // timing out after 50 * 10 ms. 50 is the signature count, to average a bit,
    // 10ms is what it may take at max in the test per sig.
    @Test(timeout = 500)
    @Ignore("This is not really a requirement but was meant to show the supperior performance of bitcoinJ")
    @Throws(Exception::class)
    fun generateSignaturesBitlib() {
        // bitlib is slow to sign. 6ms per signature. figure out how to replace that with bitcoinJ and whether that is faster.
        val requests = ArrayList<SigningRequest?>()
        for (i in 0..29) {
            val msg = "bla" + i
            requests.add(
                SigningRequest(PUBLIC_KEYS[i % COUNT]!!, HashUtils.sha256((msg).toByteArray()))
            )
        }
        StandardTransactionBuilder.generateSignatures(
            requests.filterNotNull().toTypedArray(),
            PRIVATE_KEY_RING
        )
    }

    companion object {
        private val network: NetworkParameters = NetworkParameters.testNetwork
        private const val COUNT = 9
        private val PRIVATE_KEYS = arrayOfNulls<InMemoryPrivateKey>(COUNT)
        private val PUBLIC_KEYS: Array<PublicKey?> = arrayOfNulls<PublicKey>(COUNT)
        private val ADDRS: Array<BitcoinAddress?> = arrayOfNulls<BitcoinAddress>(COUNT)
        private val UTXOS: Array<Array<UnspentTransactionOutput?>> =
            Array<Array<UnspentTransactionOutput?>>(COUNT) {
                arrayOfNulls<UnspentTransactionOutput>(2)
            }

        init {
            for (i in 0 until COUNT) {
                PRIVATE_KEYS[i] = getPrivKey("1" + i)
                PUBLIC_KEYS[i] = PRIVATE_KEYS[i]!!.publicKey
                // their addresses and 2 UTXOs each,
                ADDRS[i] = PUBLIC_KEYS[i]!!.toAddress(network, AddressType.P2PKH)
                // with values 1/3, 3/5, 7/9 and 15/17.
                UTXOS[i][0] = getUtxo(
                    ADDRS[i]!!,
                    2.0.pow(1 + i).toLong() - 1 + TransactionUtils.MINIMUM_OUTPUT_VALUE
                )
                UTXOS[i][1] = getUtxo(
                    ADDRS[i]!!,
                    2.0.pow(1 + i).toLong() + 1 + TransactionUtils.MINIMUM_OUTPUT_VALUE
                )
            }
        }

        private val PRIVATE_KEY_RING: IPrivateKeyRing = object : IPrivateKeyRing {
            override fun findSignerByPublicKey(publicKey: PublicKey?): BitcoinSigner? {
                val i = PUBLIC_KEYS.lastIndexOf(publicKey)
                if (i >= 0) {
                    return PRIVATE_KEYS[i]
                }
                return null
            }
        }

        private val KEY_RING: IPublicKeyRing = object : IPublicKeyRing {
            override fun findPublicKeyByAddress(address: BitcoinAddress?): PublicKey? {
                for (i in 0 until COUNT) {
                    if (ADDRS[i] == address) {
                        return PUBLIC_KEYS[i]
                    }
                }
                return null
            }
        }

        private fun getUtxo(address: BitcoinAddress, value: Long): UnspentTransactionOutput =
            UnspentTransactionOutput(
                OutPoint(Sha256Hash.ZERO_HASH, 0),
                0,
                value,
                ScriptOutputP2PKH(address.getTypeSpecificBytes())
            )

        /**
         * Helper to get defined public keys
         *
         * @param s one byte hex values as string representation. "00" - "ff"
         */
        private fun getPrivKey(s: String?): InMemoryPrivateKey =
            InMemoryPrivateKey(
                HexUtils.toBytes(s + "00000000000000000000000000000000000000000000000000000000000000"),
                true
            )
    }
}
