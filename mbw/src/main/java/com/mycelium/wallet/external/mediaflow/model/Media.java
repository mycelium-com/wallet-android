package com.mycelium.wallet.external.mediaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Media {
    public Media() {
    }

    public Media(int id, String sourceUrl) {
        this.id = id;
        this.sourceUrl = sourceUrl;
    }

    @JsonProperty("id")
    public int id;

    @JsonProperty("source_url")
    public String sourceUrl;
}
