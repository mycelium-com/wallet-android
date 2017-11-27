package com.mycelium.wallet.activity.rmc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */

public class EthRate {
    public EthRate() {
    }

    @JsonProperty("price")
    public EthUsdRate rate;
}
