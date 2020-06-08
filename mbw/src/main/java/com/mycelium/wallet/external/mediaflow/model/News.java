package com.mycelium.wallet.external.mediaflow.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class News implements Serializable {
    @JsonProperty("id")
    public int id;

    @JsonProperty("title")
    public Content title;

    @JsonProperty("content")
    public Content content;

    @JsonProperty("excerpt")
    public Content excerpt;

    @JsonProperty("date")
    public Date date;

    @JsonProperty("author")
    public int authorId;

    @JsonIgnore
    public Author author;

    @JsonProperty("link")
    public String link;

    @JsonProperty("jetpack_featured_media_url")
    public String image;

    @JsonProperty("categories")
    public List<Integer> categoriesIds;

    @JsonIgnore
    public List<Category> categories;

    @JsonProperty("tags")
    public List<Integer> tagsIds;

    @JsonIgnore
    public List<Tag> tags;

    @JsonIgnore
    public boolean isRead = false;

    /**
     * if news created from push notification (notification contains not full news data, content is cutoff, no date, no author etc)
     * or in other some way what can't deliver full news data we should mark it isFull = false
     */
    @JsonIgnore
    public boolean isFull = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        News news = (News) o;
        return id == news.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
