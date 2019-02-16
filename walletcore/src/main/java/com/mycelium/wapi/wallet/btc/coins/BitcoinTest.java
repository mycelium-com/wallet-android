package com.mycelium.wapi.wallet.btc.coins;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.SoftDustPolicy;
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;

public class BitcoinTest extends BitcoinBasedCryptoCurrency {
    private BitcoinTest() {
        id = "bitcoin.test";

        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 100;
        dumpedPrivateKeyHeader = 239;

        name = "Bitcoin Test";
        symbol = "BTCt";
        uriScheme = "bitcoin";
        bip44Index = 1;
        unitExponent = 8;
        feeValue = value(10000);
        minNonDust = value(5460);
        softDustLimit = value(1000000); // 0.01 BTC
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT;
        signedMessageHeader = toBytes("Bitcoin Signed Message:\n");
    }

    private static BitcoinTest instance = new BitcoinTest();
    public static synchronized CryptoCurrency get() {
        return instance;
    }

    @Override
    public GenericAddress parseAddress(String addressString) throws AddressMalformedException {
        Address address = Address.fromString(addressString);
        if (address == null) {
            return null;
        }

        if (!address.getNetwork().isTestnet())
            throw new AddressMalformedException("Address " + addressString + " is malformed");

        return new BtcAddress(this, address);
    }
}