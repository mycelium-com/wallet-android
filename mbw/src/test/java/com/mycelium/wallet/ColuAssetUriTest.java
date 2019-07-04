package com.mycelium.wallet;

import com.mrd.bitlib.model.NetworkParameters;

import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.content.GenericAssetUriParser;
import com.mycelium.wapi.content.btc.BitcoinUriParser;
import com.mycelium.wapi.content.colu.ColuAssetUri;
import com.mycelium.wapi.content.colu.mss.MSSUri;
import com.mycelium.wapi.content.colu.mss.MSSUriParser;
import com.mycelium.wapi.content.colu.mt.MTUri;
import com.mycelium.wapi.content.colu.mt.MTUriParser;
import com.mycelium.wapi.content.colu.rmc.RMCUri;
import com.mycelium.wapi.content.colu.rmc.RMCUriParser;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.coins.MASSCoinTest;
import com.mycelium.wapi.wallet.colu.coins.MTCoinTest;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;
import org.junit.Test;

import static com.mrd.bitlib.model.NetworkParameters.productionNetwork;
import static com.mrd.bitlib.model.NetworkParameters.testNetwork;
import static org.junit.Assert.*;

public class ColuAssetUriTest {
    @Test
    public void parse() {
        // ColuAccount.ColuAssetType implies that the following matchings are valid:
        // RMC = rmc
        // mss|mass = mass
        // mt = mt

        // rmc mainnet
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN", RMCCoin.INSTANCE, productionNetwork,
                new RMCUri(AddressUtils.from(RMCCoin.INSTANCE, "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), null, null));
        // mass testnet
        testParse("mss:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", MASSCoinTest.INSTANCE, testNetwork,
                new MSSUri(AddressUtils.from(MASSCoinTest.INSTANCE, "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null));
        // mycelium token testnet with ignored extra
        testParse("mt:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?foo=bla", MTCoinTest.INSTANCE, testNetwork,
                new MTUri(AddressUtils.from(MTCoinTest.INSTANCE, "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null));
        // mycelium token testnet with non-standard :// url part
        testParse("mt://mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", MTCoinTest.INSTANCE, testNetwork,
                new MTUri(AddressUtils.from(MTCoinTest.INSTANCE, "mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null));
    }

    @Test
    public void parseIntAmount() {
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=123", RMCCoin.INSTANCE, productionNetwork,
                new RMCUri(AddressUtils.from(RMCCoin.INSTANCE, "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), Value.valueOf(RMCCoin.INSTANCE, 12300000000L), null));
    }

    @Test
    public void parseFloatAmount() {
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=123.456", RMCCoin.INSTANCE, productionNetwork,
                new RMCUri(AddressUtils.from(RMCCoin.INSTANCE, "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), Value.valueOf(RMCCoin.INSTANCE, 12345600000L), null));
    }

    @Test
    public void parseWithLabel() {
        String label = "HelloWorld";
        String address = "1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN";
        testParse("rmc:" + address + "?amount=123.456&label=" + label, RMCCoin.INSTANCE, productionNetwork,
                new RMCUri(AddressUtils.from(RMCCoin.INSTANCE, address), Value.valueOf(RMCCoin.INSTANCE, 12345600000L), label));
    }

    @Test
    public void parseFailsWithUnknownToken() {
        testParse("bitc:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", BitcoinTest.get(), testNetwork, null);
    }

    @Test
    public void parseFailsWithOtherUrl() {
        testParse("bitid://bitid.bitcoin.blue/callback?x=e7befd6d54c306ef&u=1", BitcoinTest.get(), testNetwork, null);
    }

    private void testParse(String url, CryptoCurrency coinType, NetworkParameters np, GenericAssetUri expected) {
        GenericAssetUriParser parser;
        switch (coinType.getName()) {
            case "Bitcoin":
            case "Bitcoin Test":
                parser = new BitcoinUriParser(np);
                break;
            case "Mycelium Token":
            case "Mycelium Token Test":
                parser = new MTUriParser(np);
                break;
            case "Mass Token":
            case "Mass Token Test":
                parser = new MSSUriParser(np);
                break;
            case "RMC":
            case "RMC Test":
                parser = new RMCUriParser(np);
                break;
            default:
                fail();
                return;
        }
        GenericAssetUri actual = parser.parse(url);
        assertEquals(expected, actual);
    }
}