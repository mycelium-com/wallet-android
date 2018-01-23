package com.mycelium.wallet.exchange.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class CoinmarketcapRate {
    @JsonProperty("id")
    private String id;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("price_usd")
    private float priceUsd;

    @JsonProperty("price_btc")
    private float priceBtc;

    public String getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public float getPriceUsd() {
        return priceUsd;
    }

    public float getPriceBtc() {
        return priceBtc;
    }
}
