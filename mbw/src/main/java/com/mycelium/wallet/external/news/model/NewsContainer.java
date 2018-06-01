package com.mycelium.wallet.external.news.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NewsContainer {
    @JsonProperty("posts")
    public List<News> posts;
}
