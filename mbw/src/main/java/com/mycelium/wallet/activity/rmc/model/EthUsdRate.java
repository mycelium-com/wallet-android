package com.mycelium.wallet.activity.rmc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */

public class EthUsdRate {
    public EthUsdRate() {
    }

    @JsonProperty("usd")
    public double rate;
}
