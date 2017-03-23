package com.mycelium.wallet.pop;

import com.mrd.bitlib.bitcoinj.Base58;
import com.google.common.base.Strings;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
 * Proof of Payment request
 */
public class PopRequest implements Serializable {
    private byte[] n;
    private Long amountSatoshis;
    private String label;
    private String message;
    private Sha256Hash txid;
    private String p;

    public PopRequest(String input) {
        if (!input.startsWith("btcpop:?")) {
            throw new IllegalArgumentException("URI must start with 'btcpop:?':" + input);
        }

        String query = input.substring("btcpop:?".length());

        StringTokenizer parameters = new StringTokenizer(query, "&", false);
        while (parameters.hasMoreTokens()) {
            String token = parameters.nextToken();
            if (token.startsWith("=")) {
                throw new IllegalArgumentException("Empty parameter name in: " + token);
            }
            if (!token.contains("=")) {
                throw new IllegalArgumentException("No '=' in: " + token);
            }
            String[] paramPair = token.split("=");
            if (paramPair.length > 2) {
                throw new IllegalArgumentException("More than 2 '=' characters in: " + token);
            }
            String key = paramPair[0];
            String value = null;
            if (paramPair.length == 2) {
                value = PopURIDecoder.popURIDecode(paramPair[1]);
            }
            if ("n".equals(key)) {
                if (value == null) {
                    throw new IllegalArgumentException("Nonce must not be empty");
                }
                n = Base58.decode(value);
                if (n == null) {
                    throw new IllegalArgumentException("Nonce " + value + " cannot be base58 decoded");
                }
                if (n.length < 1) {
                    throw new IllegalArgumentException("Nonce too short");
                }
            } else if ("p".equals(key)) {
                if (Strings.isNullOrEmpty(value)) {
                    throw new IllegalArgumentException("Pop URL must not be empty");
                }
                p = value;
            } else if ("label".equals(key)) {
                label = value;
            } else if ("message".equals(key)) {
                message = value;
            } else if ("amount".equals(key)) {
                if (value != null) {
                    // Expect mount in BTC as in BIP0021
                    amountSatoshis = new BigDecimal(value).movePointRight(8).toBigIntegerExact().longValue();
                    if (amountSatoshis < 0) {
                        throw new IllegalArgumentException("Negative amount not allowed");
                    }
                    if (amountSatoshis > 2100000000000000L) {
                        throw new IllegalArgumentException("Too high amount: " + amountSatoshis);
                    }
                }
            } else if ("txid".equals(key)) {
                if (value == null) {
                    continue;
                }
                byte[] bytes = Base58.decode(value);
                if (bytes == null) {
                    throw new IllegalArgumentException("Can't Base58 decode value " + value);
                }
                txid = Sha256Hash.of(bytes);
            }
        }
        if (p == null || n == null) {
            throw new IllegalArgumentException("p and n must be set");
        }
    }

    public byte[] getN() {
        return n;
    }

    public Long getAmountSatoshis() {
        return amountSatoshis;
    }

    public String getLabel() {
        return label;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public String getP() {
        return p;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return "txid=" + getTxid() + ", label=" + getLabel() + ", amount=" + getAmountSatoshis();
    }
}
