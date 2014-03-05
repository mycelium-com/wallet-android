package com.mycelium.lt.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PriceFormula implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final String id;
   @JsonProperty
   public final String name;

   public PriceFormula(@JsonProperty("id") String id, @JsonProperty("name") String name) {
      this.id = id;
      this.name = name;
   }

   @Override
   public int hashCode() {
      return id.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof PriceFormula)) {
         return false;
      }
      return ((PriceFormula) obj).id.equals(id);
   }

}
