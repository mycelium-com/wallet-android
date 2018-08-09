package com.mycelium.wapi.wallet.coins;

import com.mycelium.wapi.wallet.coins.families.BitFamily;

import static com.mycelium.wapi.wallet.coins.CoinType.toBytes;
import static javax.management.Query.value;

public class BitcoinMain extends BitFamily {
    private BitcoinMain() {
        id = "bitcoin.main";

        addressHeader = 0;
        p2shHeader = 5;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        spendableCoinbaseDepth = 100;
        dumpedPrivateKeyHeader = 128;

        name = "Bitcoin";
        symbol = "BTC";
        uriScheme = "bitcoin";
        bip44Index = 0;
        unitExponent = 8;
        feeValue = value(12000);
        minNonDust = value(5460);
        softDustLimit = value(1000000); // 0.01 BTC
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT;
        signedMessageHeader = toBytes("Bitcoin Signed Message:\n");
    }

    private static BitcoinMain instance = new BitcoinMain();
    public static synchronized CoinType get() {
        return instance;
    }
}