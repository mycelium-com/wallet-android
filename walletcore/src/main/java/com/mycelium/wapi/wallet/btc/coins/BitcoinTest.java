package com.mycelium.wapi.wallet.btc.coins;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.SoftDustPolicy;
import com.mycelium.wapi.wallet.coins.families.BitcoinBasedCryptoCurrency;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;
import com.mycelium.wapi.wallet.segwit.SegwitAddress;

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

        if (address.getNetwork().isProdnet())
            throw new AddressMalformedException("Address " + addressString + " is malformed");

        return (address.getType() == AddressType.P2WPKH) ?
                new SegwitAddress(this, (com.mrd.bitlib.model.SegwitAddress) address) :
                new BtcLegacyAddress(this,
                        address.getAllAddressBytes());
    }
}