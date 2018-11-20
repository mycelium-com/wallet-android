package com.mycelium.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.content.ColuAssetUri;

import org.junit.Test;

import java.math.BigDecimal;

import static com.mrd.bitlib.model.NetworkParameters.productionNetwork;
import static com.mrd.bitlib.model.NetworkParameters.testNetwork;
import static org.junit.Assert.*;

public class ColuAssetUriTest {
    @Test
    public void from() throws Exception {
    }

    @Test
    public void parse() throws Exception {
        // ColuAccount.ColuAssetType implies that the following matchings are valid:
        // RMC = rmc
        // mss|mass = mass
        // mt = mt

        // rmc mainnet
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN", productionNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), null, null, "rmc")));
        // mass testnet
        testParse("mss:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", testNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, "mss")));
        // mycelium token testnet with ignored extra
        testParse("mt:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx?foo=bla", testNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, "mt")));
        // mycelium token testnet with non-standard :// url part
        testParse("mt://mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", testNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("mq7se9wy2egettFxPbmn99cK8v5AFq55Lx"), null, null, "mt")));
    }

    @Test
    public void parseIntAmount() {
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=123456", productionNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), BigDecimal.valueOf(123456L), null, "rmc")));
    }

    @Test
    public void parseFloatAmount() {
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=123.456", productionNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), BigDecimal.valueOf(123456L, 3), null, "rmc")));
    }

    @Test
    public void parseWithLabel() {
        testParse("rmc:1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN?amount=123.456&label=Hello World", productionNetwork,
                Optional.of(new ColuAssetUri(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), BigDecimal.valueOf(123456L, 3), "Hello World", "rmc")));
    }

    @Test
    public void parseFailsWithUnknownToken() {
        testParse("bitcoin:mq7se9wy2egettFxPbmn99cK8v5AFq55Lx", testNetwork,
                Optional.<ColuAssetUri>absent());
    }

    @Test
    public void parseFailsWithOtherUrl() {
        testParse("bitid://bitid.bitcoin.blue/callback?x=e7befd6d54c306ef&u=1", testNetwork,
                Optional.<ColuAssetUri>absent());
    }

    @Test
    public void fromAddress() throws Exception {
        ColuAssetUri actual = ColuAssetUri.fromAddress(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), "rmc");
        ColuAssetUri expected = new ColuAssetUri(Address.fromString("1A3fouaDJA4RRLnQmFxQRh98gr8cFGvwdN"), null, null, "rmc");
        assertEquals(expected.toString(), actual.toString());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void testParse(String url, NetworkParameters np, Optional<? extends ColuAssetUri> expected) {
        Optional<? extends ColuAssetUri> actual = ColuAssetUri.parse(url, np);
        assertEquals(expected.toString(), actual.toString());
    }
}