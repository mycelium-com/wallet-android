package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

import java.io.Serializable;

public interface GenericAddress extends Serializable{
    CryptoCurrency getCoinType();
    String toString();
    long getId();
    String toMultiLineString();
    String toDoubleLineString();
    String toShortString();
}
