package com.mycelium.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.content.btc.BitcoinUri;
import com.mycelium.wapi.content.btc.BitcoinUriParser;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.Value;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BitcoinUriTest {
    private static final NetworkParameters TN = NetworkParameters.testNetwork;
    private static final NetworkParameters PN = NetworkParameters.productionNetwork;

    @Test
    public void testParse() throws Exception {
        //TODO: Clarify situation with bip72
//        // from BIP72 with fallback
//        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11&r=https://merchant.com/pay.php?h=2a8628fc2fbe", TN,
//                Optional.of(BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),"mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"),
//                        Value.valueOf(BitcoinMain.get(), 11000000), null, "https://merchant.com/pay.php?h=2a8628fc2fbe"
//                )));
//        // from BIP72 without fallback
//        testParse("bitcoin:?r=https://merchant.com/pay.php?h=2a8628fc2fbe", TN,
//                Optional.of(BitcoinUri.from(null, null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe")));
//        // with mainnet
//        testParse("bitcoin:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=0.11&r=https://merchant.com/pay.php?h=2a8628fc2fbe", PN,
//                Optional.of(BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),"1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"),
//                        Value.valueOf(BitcoinMain.get(), 11000000), null, "https://merchant.com/pay.php?h=2a8628fc2fbe")));
        // with only address
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", TN,
                Optional.of(BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),"mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"),
                        null, null, null)));
        // with only address and amount
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11", TN,
                Optional.of(BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),"mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"),
                        Value.valueOf(BitcoinMain.get(), 11000000), null, null)));
    }

    private void testParse(String url, NetworkParameters np, Optional<? extends BitcoinUri> expected) {
        GenericAssetUri actual =  (new BitcoinUriParser(np)).parse(url);
        assertEquals(expected.toString(), (Optional.of(actual)).toString());
    }

    @Test
    public void testToString() {
        assertEquals("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),
                "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, null).toString());
        assertEquals("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?amount=0.11", BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),
                "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), Value.valueOf(BitcoinMain.get(), 11000000), null, null).toString());
    }

    @Test
    public void testToStringFails() {
        assertNotEquals("According to BIP72, the r-parameter should not be url encoded?", "bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe",
                BitcoinUri.from(AddressUtils.from(BitcoinMain.get(),"mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe").toString());
        assertNotEquals("bitcoin:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe", BitcoinUri.from(null, null, null, "https://merchant.com/pay.php?h=2a8628fc2fbe").toString());
    }
}
