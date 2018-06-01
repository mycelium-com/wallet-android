package com.mycelium.wallet.external.news.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class News {
    @JsonProperty("title")
    public String title;

    @JsonProperty("content")
    public String content;

    @JsonProperty("date")
    public Date date;

    @JsonProperty("author")
    public Author author;

    @JsonProperty("short_URL")
    public String link;
}
