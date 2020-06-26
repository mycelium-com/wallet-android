package com.mycelium.wallet.external.mediaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Content implements Serializable {
    @JsonProperty("rendered")
    public String rendered;

    public Content(String rendered) {
        this.rendered = rendered;
    }

    public Content() {
    }
}
