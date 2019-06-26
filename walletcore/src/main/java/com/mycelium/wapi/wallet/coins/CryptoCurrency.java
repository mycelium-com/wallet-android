package com.mycelium.wapi.wallet.coins;

import com.google.common.base.Charsets;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class CryptoCurrency extends AbstractAsset {
    private static final long serialVersionUID = 1L;

    private static final String BIP_44_KEY_PATH = "44H/%dH/%dH";

    protected String id;
    protected int addressHeader;
    protected int p2shHeader;
    protected int dumpedPrivateKeyHeader;
    protected int[] acceptableAddressCodes;
    protected int spendableCoinbaseDepth;
    protected String uriScheme;
    protected Integer bip44Index;
    protected Integer unitExponent;
    protected Integer friendlyDigits;
    protected String addressPrefix;
    protected Value feeValue;
    protected Value minNonDust;
    protected Value softDustLimit;
    protected SoftDustPolicy softDustPolicy;
    protected FeePolicy feePolicy = FeePolicy.FEE_PER_KB;
    protected byte[] signedMessageHeader;

    @Override
    public String getName() {
        return checkNotNull(name, "A coin failed to set a name");
    }

    @Override
    public String getSymbol() {
        return checkNotNull(symbol, "A coin failed to set a symbol");
    }

    @Override
    public int getUnitExponent() {
        return checkNotNull(unitExponent, "A coin failed to set a unit exponent");
    }

    @Override
    public int getFriendlyDigits() {
        return friendlyDigits;
    }

    protected static byte[] toBytes(String str) {
        return str.getBytes(Charsets.UTF_8);
    }

    /**
     Return an address prefix like NXT- or BURST-, otherwise and empty string
     */
    public String getAddressPrefix() {
        return checkNotNull(addressPrefix, "A coin failed to set the address prefix");
    }

    @Override
    public Value oneCoin() {
        if (oneCoin == null) {
            BigInteger units = BigInteger.TEN.pow(getUnitExponent());
            oneCoin = Value.valueOf(this, units.longValue());
        }
        return oneCoin;
    }

    public FeePolicy getFeePolicy() {
        return feePolicy;
    }

    @Override
    public Value value(String string) {
        return Value.parse(this, string);
    }

    @Override
    public Value value(long units) {
        return Value.valueOf(this, units);
    }

    @Override
    public String toString() {
        return "Coin{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", bip44Index=" + (bip44Index != null ?  bip44Index : "null") +
                '}';
    }

    public String getId() {
        return id;
    }
}
