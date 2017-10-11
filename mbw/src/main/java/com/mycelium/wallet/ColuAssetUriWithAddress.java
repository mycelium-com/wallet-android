package com.mycelium.wallet;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

import java.math.BigDecimal;

/**
 * Created by elvis on 27.07.17.
 */

class ColuAssetUriWithAddress extends ColuAssetUri {
    public ColuAssetUriWithAddress(Address address, BigDecimal amount, String label, String scheme) {
        super(address, amount, label, scheme);
    }

    public ColuAssetUriWithAddress(Address address, BigDecimal amount, String label, String callbackURL, String scheme) {
        super(address, amount, label, callbackURL, scheme);
    }

    public static Optional<ColuAssetUriWithAddress> parseWithAddress(String uri, NetworkParameters network) {
        Optional<? extends ColuAssetUri> bitcoinUri = ColuAssetUri.parse(uri, network);
        if (!bitcoinUri.isPresent()) {
            return Optional.absent();
        }
        return fromRmcUri(bitcoinUri.get());
    }

    public static Optional<ColuAssetUriWithAddress> fromRmcUri(ColuAssetUri bitcoinUri) {
        if (null == bitcoinUri.address) {
            return Optional.absent();
        }
        return Optional.of(new ColuAssetUriWithAddress(bitcoinUri.address, bitcoinUri.amount, bitcoinUri.label, bitcoinUri.callbackURL, bitcoinUri.scheme));
    }

    public static ColuAssetUriWithAddress fromAddress(Address address, String scheme) {
        return new ColuAssetUriWithAddress(address, null, null, scheme);
    }
}
