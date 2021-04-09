package com.mycelium.wapi.wallet.fiat.coins;

import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.coins.AbstractAsset;
import com.mycelium.wapi.wallet.coins.Value;

import java.math.BigInteger;
import java.util.Objects;

import javax.annotation.Nonnull;

public class FiatType extends AbstractAsset {

    public FiatType(String symbol) {
        this.symbol = symbol;
    }

    @Nonnull
    @Override
    public String getId() {
        return symbol;
    }

    @Nonnull
    @Override
    public String getName() {
        return symbol;
    }

    @Nonnull
    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public int getUnitExponent() {
        return 2;
    }

    @Override
    public int getFriendlyDigits() {
        return 2;
    }

    @Override
    public Address parseAddress(String address) {
        return null;
    }

    @Nonnull
    @Override
    public Value oneCoin() {
        if (oneCoin == null) {
            BigInteger units = BigInteger.TEN.pow(getUnitExponent());
            oneCoin = Value.valueOf(this, units.longValue());
        }
        return oneCoin;
    }

    @Nonnull
    @Override
    public Value value(long units) {
        return Value.valueOf(this, units);
    }

 /* TODO - implement equals
e
    @Override
    public boolean equals(AssetInfo o) {
        if (this == o) return true;
       if (o == null || getClass() != o.getClass()) return false;
       FiatType fiatType = (FiatType) o;
        return Objects.equals(symbol, fiatType.symbol);
    }
*/

    @Override
    public int hashCode() {

        return Objects.hash(symbol);
    }

    @Nonnull
    @Override
    public Value value(@Nonnull String string) {
        return Value.parse(this, string);
    }
}
