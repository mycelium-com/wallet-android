package com.mycelium.lt.api.model;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdSearchItem implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final UUID id;
   @JsonProperty
   public final AdType type;
   @JsonProperty
   public final String description;
   @JsonProperty
   public final String currency;
   @JsonProperty
   public final String alternateCurrency;
   @JsonProperty
   public final int minimumFiat;
   @JsonProperty
   public final int maximumFiat;
   @JsonProperty
   public final double oneBtcInFiat;
   @JsonProperty
   public final double oneBtcInAlternateCurrency;
   @JsonProperty
   public final GpsLocation location;
   @JsonProperty
   public final int distanceInMeters;
   @JsonProperty
   public final PublicTraderInfo traderInfo;

   public AdSearchItem(@JsonProperty("id") UUID id, @JsonProperty("type") AdType type,
         @JsonProperty("description") String description, @JsonProperty("currency") String currency,
         @JsonProperty("alternateCurrency") String alternateCurrency, @JsonProperty("minimumFiat") int minimumFiat,
         @JsonProperty("maximumFiat") int maximumFiat, @JsonProperty("oneBtcInFiat") double oneBtcInFiat,
         @JsonProperty("oneBtcInAlternateCurrency") double oneBtcInAlternateCurrency,
         @JsonProperty("location") GpsLocation location, @JsonProperty("distanceInMeters") int distanceInMeters,
         @JsonProperty("traderInfo") PublicTraderInfo traderInfo) {
      this.id = id;
      this.type = type;
      this.description = description;
      this.currency = currency;
      this.alternateCurrency = alternateCurrency;
      this.minimumFiat = minimumFiat;
      this.maximumFiat = maximumFiat;
      this.oneBtcInFiat = oneBtcInFiat;
      this.oneBtcInAlternateCurrency = oneBtcInAlternateCurrency;
      this.location = location;
      this.distanceInMeters = distanceInMeters;
      this.traderInfo = traderInfo;
   }

}
