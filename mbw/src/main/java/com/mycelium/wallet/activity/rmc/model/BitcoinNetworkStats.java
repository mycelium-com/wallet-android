package com.mycelium.wallet.activity.rmc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BitcoinNetworkStats {
    public BitcoinNetworkStats() {
    }

    @JsonProperty("difficulty")
    public long difficulty;
}
