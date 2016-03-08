package com.mycelium.wallet.glidera.api.response;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class GlideraResponse {
    @Override
    public String toString() {
        return new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(this).toString();
    }
}
