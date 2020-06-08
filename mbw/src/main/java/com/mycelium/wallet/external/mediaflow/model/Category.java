package com.mycelium.wallet.external.mediaflow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

public class Category implements Serializable {
    public Category(String name) {
        this.name = name;
    }

    public Category() {
    }

    @JsonProperty("id")
    public int id;

    @JsonProperty("name")
    public String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
