package com.mycelium.wallet.exchange.model;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;

public class Rate extends GenericJson {
    @Key("pair")
    public String pair;

    @Key("buy")
    public float buy;

    @Key("sell")
    public float sell;
}
