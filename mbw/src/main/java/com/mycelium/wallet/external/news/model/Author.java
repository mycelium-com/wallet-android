package com.mycelium.wallet.external.news.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Author {
    @JsonProperty("avatar_URL")
    public String avatar;
    @JsonProperty("name")
    public String name;
}
