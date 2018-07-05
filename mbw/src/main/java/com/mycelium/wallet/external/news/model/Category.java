package com.mycelium.wallet.external.news.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Category implements Serializable {
    public Category(String name) {
        this.name = name;
    }

    public Category() {
    }

    @JsonProperty("name")
    public String name;
}
