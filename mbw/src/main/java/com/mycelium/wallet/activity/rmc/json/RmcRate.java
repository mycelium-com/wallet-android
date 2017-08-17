package com.mycelium.wallet.activity.rmc.json;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;

/**
 * Created by elvis on 16.08.17.
 */

public class RmcRate extends GenericJson {
    @Key("rate")
    @JsonString
    public Float rate;
}
