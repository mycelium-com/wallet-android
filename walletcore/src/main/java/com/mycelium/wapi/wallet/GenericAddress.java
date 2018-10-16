package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;

import java.io.Serializable;

public interface GenericAddress extends Serializable{
    CryptoCurrency getCoinType();
    AddressType getType();
    long getId();
    HdKeyPath getBip32Path();
}
