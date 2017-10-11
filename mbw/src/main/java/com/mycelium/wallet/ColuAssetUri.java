package com.mycelium.wallet;

import android.net.Uri;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

import java.io.Serializable;
import java.math.BigDecimal;

public class ColuAssetUri implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Address address;
    public final BigDecimal amount;
    public final String label;
    public final String callbackURL;
    public final String scheme;

    public static ColuAssetUri from(Address address, BigDecimal amount, String label, String callbackURL, String scheme) {
        if (address != null) {
            return new ColuAssetUriWithAddress(address, amount, label, callbackURL, scheme);
        } else {
            return new ColuAssetUri(null, amount, label, callbackURL, scheme);
        }
    }

    public ColuAssetUri(Address address, BigDecimal amount, String label, String scheme) {
        this(address, amount, label, null, scheme);
    }

    public ColuAssetUri(Address address, BigDecimal amount, String label, String callbackURL, String scheme) {
        this.address = address;
        this.amount = amount;
        this.label = label == null ? null : label.trim();
        this.callbackURL = callbackURL;
        this.scheme = scheme;
    }

    public static Optional<? extends ColuAssetUri> parse(String uri, NetworkParameters network) {
        try {
            Uri u = Uri.parse(uri.trim());
            String scheme = u.getScheme();

            String schemeSpecific = u.getSchemeSpecificPart();
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid bitcoin URI in the form "bitcoin://"
                schemeSpecific = schemeSpecific.substring(2);
            }

            u = Uri.parse(scheme + "://" + schemeSpecific);

            // Address
            Address address = null;
            String addressString = u.getHost();
            if (addressString != null && addressString.length() > 0) {
                address = Address.fromString(addressString.trim(), network);
            }

            // Amount
            String amountStr = u.getQueryParameter("amount");
            BigDecimal amount = null;
            if (amountStr != null) {
                amount = new BigDecimal(amountStr);
            }

            // Label
            // Bip21 defines "?label" and "?message" - lets try "label" first and if it does not
            // exist, lets use "message"
            String label = u.getQueryParameter("label");
            if (label == null) {
                label = u.getQueryParameter("message");
            }

            // Check if the supplied "address" is actually an encrypted private key
            if (Bip38.isBip38PrivateKey(addressString)) {
                return Optional.of(new PrivateKeyUri(addressString, label, scheme));
            }

            // Payment Uri
            String paymentUri = u.getQueryParameter("r");

            if (address == null && paymentUri == null) {
                // not a valid bitcoin uri
                return Optional.absent();
            }

            return Optional.of(new ColuAssetUri(address, amount, label, paymentUri, scheme));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public static ColuAssetUri fromAddress(Address address, String scheme) {
        return new ColuAssetUri(address, null, null, scheme);
    }

    public String toString() {
        Uri.Builder builder = new Uri.Builder()
                .scheme(scheme)
                .authority(address == null ? "" : address.toString());
        if (amount != null) {
            builder.appendQueryParameter("amount", amount.stripTrailingZeros().toPlainString());
        }
        if (label != null) {
            builder.appendQueryParameter("label", label);
        }
        if (callbackURL != null) {
            // TODO: 8/8/16 according to BIP72, this url should not be escaped. As so far Mycelium doesn't create r-parameter qr-codes, there is no problem.
            builder.appendQueryParameter("r", callbackURL);
        }
        //todo: this can probably be solved nicer with some opaque flags or something
        return builder.toString().replace("/", "");
    }

    public static class PrivateKeyUri extends ColuAssetUri {
        public final String keyString;

        private PrivateKeyUri(String keyString, String label, String scheme) {
            super(null, null, label, scheme);
            this.keyString = keyString;
        }
    }
}
