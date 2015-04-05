package com.mycelium.wallet.pop;

import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.StringTokenizer;

public class PopRequest implements Serializable {
    private Long nonce;
    private Long amountSatoshis;
    private String text;
    private Sha256Hash txid;
    private String url;

    public PopRequest(String input) {
        if (!input.startsWith("btcpop:?")) {
            throw new IllegalArgumentException("URI must start with 'btcpop:?':" + input);
        }

        String query = input.substring("btcpop:?".length());
        if (query == null) {
            throw new IllegalArgumentException("No query string");
        }
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
                try {
                    value = URLDecoder.decode(paramPair[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException("UTF-8 not supported!");
                }
            }

            if ("nonce".equals(key)) {
                if (value == null) {
                    throw new IllegalArgumentException("Nonce must not be empty");
                }
                nonce = Long.parseLong(value);
                if (nonce < 0) {
                    throw new IllegalArgumentException("Negative nonce not allowed");
                }
            } else if ("p".equals(key)) {
                if (value == null) {
                    throw new IllegalArgumentException("Pop URL must not be empty");
                }
                url = value;
            } else if ("text".equals(key)) {
                text = value;
            } else if ("amount".equals(key)) {
                if (value != null) {
                    // Expect mount in BTC as in BIP0021
                    amountSatoshis = new BigDecimal(value).movePointRight(8).toBigIntegerExact().longValue();
                    if (amountSatoshis < 0) {
                        throw new IllegalArgumentException("Negative amount not allowed");
                    }
                }
            } else if ("txid".equals(key)) {
                if (value != null && value.length() != 64) {
                    throw new IllegalArgumentException("Wrong length of txid. Expected 64: " + value);
                }
                txid = Sha256Hash.fromString(value);
            }
        }
        if (url == null) {
            throw new IllegalArgumentException("Pop URL must be set");
        }
        if (nonce == null) {
            throw new IllegalArgumentException("Nonce must be set");
        }
    }

    public Long getNonce() {
        return nonce;
    }

    public Long getAmountSatoshis() {
        return amountSatoshis;
    }

    public String getText() {
        return text;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public String getUrl() {
        return url;
    }

    public String toString() {
        return "txid=" + getTxid() + ", text=" + getText() + ", amount=" + getAmountSatoshis();
    }
}
