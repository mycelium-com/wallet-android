package com.mycelium.wapi.content.eth

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.eth.coins.EthTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

class EthUriParserTest {
    private val parser = EthUriParser(NetworkParameters.testNetwork, emptyMap())

    @Test
    fun plainAddressAndValue() {
        val uri = parser.parse("ethereum:$CHECKSUMMED?value=1e15") as EthUri
        assertEquals(CHECKSUMMED.lowercase(), uri.address.toString().lowercase())
        assertEquals(BigInteger.TEN.pow(15), uri.value!!.value)
    }

    @Test
    fun chainIdOfThisNetworkIsAccepted() {
        val uri = parser.parse("ethereum:$CHECKSUMMED@11155111?value=1e15") as EthUri
        assertEquals(CHECKSUMMED.lowercase(), uri.address.toString().lowercase())
        assertEquals(BigInteger.TEN.pow(15), uri.value!!.value)
    }

    @Test
    fun chainIdOfAnotherNetworkIsRejected() {
        assertNull(parser.parse("ethereum:$CHECKSUMMED@1?value=1e15"))
    }

    @Test
    fun payPrefix() {
        assertNotNull(parser.parse("ethereum:pay-$CHECKSUMMED@11155111"))
    }

    @Test
    fun tokenTransfer() {
        val uri = parser.parse("ethereum:$TOKEN@11155111/transfer?address=$CHECKSUMMED&uint256=2500000") as EthUri
        assertEquals(CHECKSUMMED.lowercase(), uri.address.toString().lowercase())
        assertEquals(TOKEN, uri.asset)
        assertEquals(BigInteger.valueOf(2500000), uri.value!!.value)
    }

    @Test
    fun unknownFunctionIsRejected() {
        assertNull(parser.parse("ethereum:$TOKEN/approve?address=$CHECKSUMMED&uint256=1"))
    }

    @Test
    fun badChecksumIsRejected() {
        // last character changed: the mixed-case checksum no longer matches
        assertNull(parser.parse("ethereum:0xad7A4b1172bCe61E7590eF685C479DBCb763ACFc"))
        assertNull(EthTest.parseAddress("0xad7A4b1172bCe61E7590eF685C479DBCb763ACFc"))
    }

    @Test
    fun addressWithoutChecksumIsAccepted() {
        assertNotNull(EthTest.parseAddress(CHECKSUMMED.lowercase()))
        assertNotNull(EthTest.parseAddress(CHECKSUMMED))
    }

    companion object {
        private const val CHECKSUMMED = "0xad7A4b1172bCe61E7590eF685C479DBCb763ACFb"
        private const val TOKEN = "0xCaC524BcA292aaade2DF8A05cC58F0a65B1B3bB9"
    }
}
