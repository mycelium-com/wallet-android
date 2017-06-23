package com.mycelium.wallet.activity.rmc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by elvis on 23.06.17.
 */

public class EthUsdRate {
    public EthUsdRate() {
    }

    @JsonProperty("usd")
    public double rate;
}
