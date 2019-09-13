package com.mycelium.wapi.content.btc

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.coins.Value
import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

class BitcoinUriTest {
    @Test
    fun testParseBIP72withFallback() {
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11&r=https://merchant.com/pay.php?h=2a8628fc2fbe", TN,
                BitcoinUri.from(AddressUtils.from(BitcoinTest.get(), "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"),
                        Value.valueOf(BitcoinTest.get(), 11000000), null, "https://merchant.com/pay.php?h=2a8628fc2fbe"
                ))
    }

    @Test
    fun testParseBIP72withoutFallback() {
        testParse("bitcoin:?r=https://merchant.com/pay.php?h=2a8628fc2fbe", TN,
                BitcoinUri.from(null, null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe"))
    }

    @Test
    fun testParseMainnetBip72() {
        testParse("bitcoin:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=0.11&r=https://merchant.com/pay.php?h=2a8628fc2fbe", PN,
                BitcoinUri.from(AddressUtils.from(BitcoinMain.get(), "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"),
                        Value.valueOf(BitcoinMain.get(), 11000000), null, "https://merchant.com/pay.php?h=2a8628fc2fbe"))
    }

    @Test
    fun testTestnetAddressOnly() {
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", TN,
                BitcoinUri.from(AddressUtils.from(BitcoinTest.get(), "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, null))
    }

    @Test
    fun testMainnetAddressLabelWithPercentageEncoding() {
        testParse("bitcoin:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?label=counterparty%3A%20burn%20address", PN,
                BitcoinUri.from(AddressUtils.from(BitcoinMain.get(), "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), null, "counterparty: burn address", null))
    }

    @Test
    fun testMainnetBip21Request50BTCWithMessage() {
        testParse("bitcoin:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=50&label=Luke-Jr&message=Donation%20for%20project%20xyz", PN,
                BitcoinUri.from(AddressUtils.from(BitcoinMain.get(), "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), Value.valueOf(BitcoinMain.get(), 5000000000), "Luke-Jr", null))
    }

    @Test
    fun testTestnetAddressAmount() {
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11", TN,
                BitcoinUri.from(AddressUtils.from(BitcoinTest.get(), "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"),
                        Value.valueOf(BitcoinMain.get(), 11000000), null, null))
    }

    @Test
    fun testTestnetAddressInvalidForm() {
        // invalid form that starts with "bitcoin://"
        testParse("bitcoin://mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", TN,
                BitcoinUri.from(AddressUtils.from(BitcoinTest.get(), "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"),
                        null, null, null))
    }

    private fun testParse(url: String, np: NetworkParameters, expected: BitcoinUri) {
        val actual = BitcoinUriParser(np).parse(url)
        assertEquals("$expected", "$actual")
    }

    @Test
    fun testToString() {
        assertEquals("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", BitcoinUri.from(AddressUtils.from(BitcoinTest.get(),
                "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, null).toString())
        assertEquals("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11", BitcoinUri.from(AddressUtils.from(BitcoinTest.get(),
                "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), Value.valueOf(BitcoinMain.get(), 11000000), null, null).toString())
    }

    @Test
    fun testToStringFails() {
        assertNotEquals("According to BIP72, the r-parameter should not be url encoded?", "bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe",
                BitcoinUri.from(AddressUtils.from(BitcoinMain.get(), "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe").toString())
        assertNotEquals("bitcoin:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", BitcoinUri.from(null, null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe").toString())
    }

    companion object {
        private val TN = NetworkParameters.testNetwork
        private val PN = NetworkParameters.productionNetwork
    }
}
