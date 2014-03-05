package com.mycelium.lt.api.model;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SellOrder implements Serializable {
   private static final long serialVersionUID = 1L;
   
   @JsonProperty
   public final UUID id;
   @JsonProperty
   public GpsLocation location;
   @JsonProperty
   public String currency;
   @JsonProperty
   public int minimumFiat;
   @JsonProperty
   public int maximumFiat;
   @JsonProperty
   public PriceFormula priceFormula;
   @JsonProperty
   public double premium;
   @JsonProperty
   public String description;
   @JsonProperty
   public boolean isActive;
   @JsonProperty
   public long deactivationTime;

   public SellOrder(@JsonProperty("id") UUID id, @JsonProperty("location") GpsLocation location,
         @JsonProperty("currency") String currency, @JsonProperty("minimumFiat") int minimumFiat,
         @JsonProperty("maximumFiat") int maximumFiat, @JsonProperty("priceFormula") PriceFormula priceFormula,
         @JsonProperty("premium") double premium, @JsonProperty("description") String description,
         @JsonProperty("isActive") boolean isActive, @JsonProperty("deactivationTime") long deactivationTime) {
      this.id = id;
      this.location = location;
      this.currency = currency;
      this.minimumFiat = minimumFiat;
      this.maximumFiat = maximumFiat;
      this.priceFormula = priceFormula;
      this.premium = premium;
      this.description = description;
      this.isActive = isActive;
      this.deactivationTime = deactivationTime;
   }

}
