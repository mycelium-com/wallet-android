package com.mycelium.wallet.exchange;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;

/**
 * Created by elvis on 10.10.17.
 */

public class Rate extends GenericJson {
    @Key("pair")
    public String pair;

    @Key("buy")
    public float buy;

    @Key("sell")
    public float sell;
}
