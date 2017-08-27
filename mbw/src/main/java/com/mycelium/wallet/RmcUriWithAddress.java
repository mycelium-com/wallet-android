package com.mycelium.wallet;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

import java.math.BigDecimal;

/**
 * Created by elvis on 27.07.17.
 */

class RmcUriWithAddress extends RmcUri {
    public RmcUriWithAddress(Address address, BigDecimal amount, String label) {
        super(address, amount, label);
    }

    public RmcUriWithAddress(Address address, BigDecimal amount, String label, String callbackURL) {
        super(address, amount, label, callbackURL);
    }

    public static Optional<RmcUriWithAddress> parseWithAddress(String uri, NetworkParameters network) {
        Optional<? extends RmcUri> bitcoinUri = RmcUri.parse(uri, network);
        if (!bitcoinUri.isPresent()) {
            return Optional.absent();
        }
        return fromRmcUri(bitcoinUri.get());
    }

    public static Optional<RmcUriWithAddress> fromRmcUri(RmcUri bitcoinUri) {
        if (null == bitcoinUri.address) {
            return Optional.absent();
        }
        return Optional.of(new RmcUriWithAddress(bitcoinUri.address, bitcoinUri.amount, bitcoinUri.label, bitcoinUri.callbackURL));
    }

    public static RmcUriWithAddress fromAddress(Address address) {
        return new RmcUriWithAddress(address, null, null);
    }
}
