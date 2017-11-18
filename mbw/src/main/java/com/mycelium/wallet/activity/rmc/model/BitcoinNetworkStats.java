package com.mycelium.wallet.activity.rmc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by elvis on 17.11.17.
 */

public class BitcoinNetworkStats {
    public BitcoinNetworkStats() {
    }

    @JsonProperty("difficulty")
    public long difficulty;
}
