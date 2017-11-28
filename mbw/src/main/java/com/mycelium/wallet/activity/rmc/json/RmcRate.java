package com.mycelium.wallet.activity.rmc.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;

public class RmcRate extends GenericJson {
    @Key("rate")
    @JsonString
    public Float rate;
}
