package com.mycelium.wallet.external.mediaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Tag implements Serializable {
    @JsonProperty("id")
    public int id;

    @JsonProperty("name")
    public String name;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }
}
