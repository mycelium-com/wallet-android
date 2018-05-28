package com.megiontechnologies;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;


/**
 * a core Bitcoin Cash Value representation, caputuring many domain specific aspects
 * of it. introduced to reduce the ambiguity when dealing with double,
 * BigInteger, long, or even worse, integer representations
 */
 public final class BitcoinCash extends BitcoinBase {
    private static final long serialVersionUID = 1L;

    public static final String BITCOIN_CASH_SYMBOL = "BCH"; // BCH

    /**
     * @param bch double Value in full bitcoins. must be an exact represenatation
     * @return bitcoin cash value representation
     * @throws IllegalArgumentException if the given double value loses precision when converted to
     *                                  long
     */
    public static BitcoinCash valueOf(double bch) {
        return valueOf(toLongExact(bch));
    }

    public static BitcoinCash valueOf(String bch) {
        return BitcoinCash.valueOf(new BigDecimal(bch).multiply(SATOSHIS_PER_BITCOIN_BD).longValueExact());
    }

    public static BitcoinCash nearestValue(double v) {
        return new BitcoinCash(Math.round(v * SATOSHIS_PER_BITCOIN));
    }

    public static BitcoinCash nearestValue(BigDecimal bitcoinAmount) {
        BigDecimal satoshis = bitcoinAmount.multiply(SATOSHIS_PER_BITCOIN_BD);
        long satoshisExact = satoshis.setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new BitcoinCash(satoshisExact);
    }

    public static BitcoinCash valueOf(long satoshis) {
        return new BitcoinCash(satoshis);
    }

    private BitcoinCash(long satoshis) {
        if (satoshis < 0)
            throw new IllegalArgumentException(String.format("Bitcoin Cash values must be debt-free and positive, but was %s",
                    satoshis));
        if (satoshis >= MAX_VALUE)
            throw new IllegalArgumentException(String.format(
                    "Bitcoin values must be smaller than 21 Million BCH, but was %s", satoshis));
        this.satoshis = satoshis;
    }

    protected BitcoinCash parse(String input) {
        return BitcoinCash.valueOf(input);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BitcoinCash bchs = (BitcoinCash) o;

        return satoshis == bchs.satoshis;
    }

    @Override
    public String toCurrencyString() {
        return BITCOIN_CASH_SYMBOL + ' ' + toString();
    }

    @Override
    public String toCurrencyString(int decimals) {
        return BITCOIN_CASH_SYMBOL + ' ' + toString(decimals);
    }
}