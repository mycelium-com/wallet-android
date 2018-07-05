package com.mycelium.wallet.external.news.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class News implements Serializable {
    @JsonProperty("ID")
    public int id;

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

    @JsonProperty("categories")
    public Map<String, Category> categories;
}
