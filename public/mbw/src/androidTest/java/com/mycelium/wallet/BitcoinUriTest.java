package com.mycelium.wallet;

import android.util.Log;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

import junit.framework.TestCase;

public class BitcoinUriTest extends TestCase {
    private static final NetworkParameters TN = NetworkParameters.testNetwork;
    private static final NetworkParameters PN = NetworkParameters.productionNetwork;

    public void testFrom() throws Exception {
    }

    public void testParse() throws Exception {
        // from BIP72 with fallback
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11&r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", TN,
                Optional.of(new BitcoinUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), 11000000L, null, "https://merchant.com/pay.php?h=2a8628fc2fbe")));
        // from BIP72 without fallback
        testParse("bitcoin:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", TN,
                Optional.of(new BitcoinUri(null, null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe")));
        // with mainnet
        testParse("bitcoin:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=0.11&r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", PN,
                Optional.of(new BitcoinUri(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), 11000000L, null, "https://merchant.com/pay.php?h=2a8628fc2fbe")));
        // with only address
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", TN,
                Optional.of(new BitcoinUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, null)));
        // with only address and amount
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11", TN,
                Optional.of(new BitcoinUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), 11000000L, null, null)));
    }

    private void testParse(String url, NetworkParameters np, Optional<? extends BitcoinUri> expected) {
        Log.d(BitcoinUriTest.class.getName(), "testParse: " + expected.toString());
        Optional<? extends BitcoinUri> actual = BitcoinUri.parse(url, np);
        assertEquals(expected.toString(), actual.toString());
    }

    public void testFromAddress() throws Exception {
    }

    public void testToString() throws Exception {
        assertEquals("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", new BitcoinUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, null).toString());
        assertEquals("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11", new BitcoinUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), 11000000L, null, null).toString());
    }

    public void testToStringFails() throws Exception {
        assertEquals("According to BIP72, the r-parameter should not be url encoded?", "bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", new BitcoinUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe").toString());
        assertEquals("bitcoin:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", new BitcoinUri(null, null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe").toString());
    }
}
