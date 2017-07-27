package com.mycelium.wallet;

import android.net.Uri;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.CoinUtil;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by elvis on 27.07.17.
 */

public class RmcUri implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Address address;
    public final BigDecimal amount;
    public final String label;
    public final String callbackURL;

    // returns a BitcoinUriWithAddress if address != null
    public static RmcUri from(Address address, BigDecimal amount, String label, String callbackURL) {
        if (address != null) {
            return new RmcUriWithAddress(address, amount, label, callbackURL);
        } else {
            return new RmcUri(null, amount, label, callbackURL);
        }
    }

    public RmcUri(Address address, BigDecimal amount, String label) {
        this(address, amount, label, null);
    }

    public RmcUri(Address address, BigDecimal amount, String label, String callbackURL) {
        this.address = address;
        this.amount = amount;
        this.label = label == null ? null : label.trim();
        this.callbackURL = callbackURL;
    }

    public static Optional<? extends RmcUri> parse(String uri, NetworkParameters network) {
        try {
            Uri u = Uri.parse(uri.trim());
            String scheme = u.getScheme();
            if (!scheme.equalsIgnoreCase("RMC")) {
                // not a bitcoin URI
                return Optional.absent();
            }
            String schemeSpecific = u.getSchemeSpecificPart();
            if (schemeSpecific.startsWith("//")) {
                // Fix for invalid bitcoin URI in the form "bitcoin://"
                schemeSpecific = schemeSpecific.substring(2);
            }
            u = Uri.parse("RMC://" + schemeSpecific);

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
                return Optional.of(new PrivateKeyUri(addressString, label));
            }

            // Payment Uri
            String paymentUri = u.getQueryParameter("r");

            if (address == null && paymentUri == null) {
                // not a valid bitcoin uri
                return Optional.absent();
            }

            return Optional.of(new RmcUri(address, amount, label, paymentUri));
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public static RmcUri fromAddress(Address address) {
        return new RmcUri(address, null, null);
    }

    public String toString() {
        Uri.Builder builder = new Uri.Builder()
                .scheme("RMC")
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

    public static class PrivateKeyUri extends RmcUri {
        public final String keyString;

        private PrivateKeyUri(String keyString, String label) {
            super(null, null, label);
            this.keyString = keyString;
        }
    }
}
