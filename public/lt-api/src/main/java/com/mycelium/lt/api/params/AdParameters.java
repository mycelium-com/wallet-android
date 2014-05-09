package com.mycelium.lt.api.params;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.lt.api.model.GpsLocation;

//todo rename TadeParameters / TraderParameters confusing
public class AdParameters implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public AdType type;
   @JsonProperty
   public GpsLocation location;
   @JsonProperty
   public String currency;
   @JsonProperty
   public int minimumFiat;
   @JsonProperty
   public int maximumFiat;
   @JsonProperty
   public String priceFormulaId;
   @JsonProperty
   public double premium;
   @JsonProperty
   public String description;

   public AdParameters(@JsonProperty("type") AdType type, @JsonProperty("location") GpsLocation location,
         @JsonProperty("currency") String currency, @JsonProperty("minimumFiat") int minimumFiat,
         @JsonProperty("maximumFiat") int maximumFiat, @JsonProperty("priceFormulaId") String priceFormulaId,
         @JsonProperty("premium") double premium, @JsonProperty("description") String description) {
      this.type = type;
      this.location = location;
      this.currency = currency;
      this.minimumFiat = minimumFiat;
      this.maximumFiat = maximumFiat;
      this.priceFormulaId = priceFormulaId;
      this.premium = premium;
      this.description = description;
   }

   @SuppressWarnings("unused")
   private AdParameters() {
      // For Jackson
   }

}
