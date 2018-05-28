package com.mycelium.wallet.external.changelly.bch;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

import org.junit.Assert;
import org.junit.Test;

public class BCHBechAddressTest {
    @Test
    public void P2PKHAddressTest() throws Exception {
        Assert.assertEquals(Address.fromStandardBytes(BCHBechAddress.bchBechDecode("bitcoincash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a").getHash(),
                NetworkParameters.productionNetwork).toString(), "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu");
    }

    @Test
    public void P2SHAddressTest() throws Exception {
        Assert.assertEquals(Address.fromP2SHBytes(BCHBechAddress.bchBechDecode("bitcoincash:pqq3728yw0y47sqn6l2na30mcw6zm78dzq5ucqzc37").getHash(),
                NetworkParameters.productionNetwork).toString(), "31nwvkZwyPdgzjBJZXfDmSWsC4ZLKpYyUw");
    }

    @Test
    public void legacyAddressConstructorTest() throws Exception {
        // Check P2PKH construct
        Assert.assertEquals(BCHBechAddress.bchBechDecode("bitcoincash:qpm2qsznhks23z7629mms6s4cwef74vcwvy22gdx6a")
                .constructLegacyAddress(NetworkParameters.productionNetwork).toString(), "1BpEi6DfDAUFd7GtittLSdBeYJvcoaVggu");
        // Check P2SH construct
        Assert.assertEquals(BCHBechAddress.bchBechDecode("bitcoincash:pqq3728yw0y47sqn6l2na30mcw6zm78dzq5ucqzc37")
                .constructLegacyAddress(NetworkParameters.productionNetwork).toString(), "31nwvkZwyPdgzjBJZXfDmSWsC4ZLKpYyUw");
    }
}
