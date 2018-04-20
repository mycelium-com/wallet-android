package com.mycelium.wallet.external.changelly.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {
    @JsonProperty("exchange_provider")
    public String provider = "Changelly";
    @JsonProperty("exchange_rate")
    public String rate;
    @JsonProperty("exchanging_amount")
    public String exchangingAmount;
    @JsonProperty("exchanging_currency")
    public String exchangingCurrency;
    @JsonProperty("receiving_address")
    public String receivingAddress;
    @JsonProperty("receiving_amount")
    public String receivingAmount;
    @JsonProperty("receiving_currency")
    public String receivingCurrency;
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("tx_id")
    public String transactionId;
    @JsonProperty("order_id")
    public String order_id;
}
