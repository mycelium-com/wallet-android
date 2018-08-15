package com.mycelium.wapi.wallet.coins;

import com.google.common.base.Charsets;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.MonetaryFormat;
import com.mycelium.wapi.wallet.coins.families.Families;
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class CoinType implements ValueType {
    private static final long serialVersionUID = 1L;

    private static final String BIP_44_KEY_PATH = "44H/%dH/%dH";

    protected String id;
    protected int addressHeader;
    protected int p2shHeader;
    protected int dumpedPrivateKeyHeader;
    protected int[] acceptableAddressCodes;
    protected int spendableCoinbaseDepth;
    protected Families family;
    protected String name;
    protected String symbol;
    protected String uriScheme;
    protected Integer bip44Index;
    protected Integer unitExponent;
    protected String addressPrefix;
    protected Value feeValue;
    protected Value minNonDust;
    protected Value softDustLimit;
    protected SoftDustPolicy softDustPolicy;
    protected FeePolicy feePolicy = FeePolicy.FEE_PER_KB;
    protected byte[] signedMessageHeader;

    private transient MonetaryFormat friendlyFormat;
    private transient MonetaryFormat plainFormat;
    private transient Value oneCoin;

    private static FeeProvider feeProvider = null;

    @Override
    public String getName() {
        return checkNotNull(name, "A coin failed to set a name");
    }

    public boolean isTestnet() {
        return id.endsWith("test");
    }

    @Override
    public String getSymbol() {
        return checkNotNull(symbol, "A coin failed to set a symbol");
    }

    public String getUriScheme() {
        return checkNotNull(uriScheme, "A coin failed to set a URI scheme");
    }

    public int getBip44Index() {
        return checkNotNull(bip44Index, "A coin failed to set a BIP 44 index");
    }

    @Override
    public int getUnitExponent() {
        return checkNotNull(unitExponent, "A coin failed to set a unit exponent");
    }

    public Value getFeeValue() {
        if (feeProvider != null) {
            return feeProvider.getFeeValue(this);
        } else {
            return getDefaultFeeValue();
        }
    }

    public Value getDefaultFeeValue() {
        return checkNotNull(feeValue, "A coin failed to set a fee value");
    }

    @Override
    public Value getMinNonDust() {
        return checkNotNull(minNonDust, "A coin failed to set a minimum amount to be considered not dust");
    }

    public Value getSoftDustLimit() {
        return checkNotNull(softDustLimit, "A coin failed to set a soft dust limit");
    }

    public SoftDustPolicy getSoftDustPolicy() {
        return checkNotNull(softDustPolicy, "A coin failed to set a soft dust policy");
    }

    public FeePolicy getFeePolicy() {
        return checkNotNull(feePolicy, "A coin failed to set a fee policy");
    }

    public byte[] getSignedMessageHeader() {
        return checkNotNull(signedMessageHeader, "A coin failed to set signed message header bytes");
    }

    public boolean canSignVerifyMessages() {
        return signedMessageHeader != null;
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

    public abstract GenericAddress newAddress(String addressStr) throws AddressMalformedException;

    @Override
    public Value oneCoin() {
        if (oneCoin == null) {
            BigInteger units = BigInteger.TEN.pow(getUnitExponent());
            oneCoin = Value.valueOf(this, units.longValue());
        }
        return oneCoin;
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
                ", bip44Index=" + bip44Index +
                '}';
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        if (friendlyFormat == null) {
            friendlyFormat = new MonetaryFormat()
                    .shift(0).minDecimals(2).code(0, symbol).postfixCode();
            switch (unitExponent) {
                case 8:
                    friendlyFormat = friendlyFormat.optionalDecimals(2, 2, 2);
                    break;
                case 6:
                    friendlyFormat = friendlyFormat.optionalDecimals(2, 2);
                    break;
                case 4:
                    friendlyFormat = friendlyFormat.optionalDecimals(2);
                    break;
                default:
                    friendlyFormat = friendlyFormat.minDecimals(unitExponent);
            }
        }
        return friendlyFormat;
    }

    @Override
    public MonetaryFormat getPlainFormat() {
        if (plainFormat == null) {
            plainFormat = new MonetaryFormat().shift(0)
                    .minDecimals(0).repeatOptionalDecimals(1, unitExponent).noCode();
        }
        return plainFormat;
    }

    @Override
    public boolean equals(ValueType obj) {
        return super.equals(obj);
    }

    public static void setFeeProvider(FeeProvider feeProvider) {
        CoinType.feeProvider = feeProvider;
    }

    public interface FeeProvider {
        Value getFeeValue(CoinType type);
    }

    public String getId() {
        return id;
    }
}
