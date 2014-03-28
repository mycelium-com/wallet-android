package com.mycelium.lt.api.model;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.crypto.PublicKey;

public class SellOrderSearchItem implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final UUID id;
   @JsonProperty
   public final String nickname;
   @JsonProperty
   public final PublicKey publicKey;
   @JsonProperty
   public final String description;
   @JsonProperty
   public final int successfulSales;
   @JsonProperty
   public final int abortedSales;
   @JsonProperty
   public final int successfulBuys;
   @JsonProperty
   public final int abortedBuys;
   @JsonProperty
   public final String currency;
   @JsonProperty
   public final int minimumFiat;
   @JsonProperty
   public final int maximumFiat;
   @JsonProperty
   public final double oneBtcInFiat;
   @JsonProperty
   public final GpsLocation location;
   @JsonProperty
   public final int distanceInMeters;
   @JsonProperty
   public final long traderAgeMs;
   @JsonProperty
   public final Long tradeMedianMs;

   public SellOrderSearchItem(@JsonProperty("id") UUID id, @JsonProperty("nickname") String nickname,
         @JsonProperty("publicKey") PublicKey publicKey, @JsonProperty("description") String description,
         @JsonProperty("successfulSales") int successfulSales, @JsonProperty("abortedSales") int abortedSales,
         @JsonProperty("successfulBuys") int successfulBuys, @JsonProperty("abortedBuys") int abortedBuys,
         @JsonProperty("currency") String currency, @JsonProperty("minimumFiat") int minimumFiat,
         @JsonProperty("maximumFiat") int maximumFiat, @JsonProperty("oneBtcInFiat") double oneBtcInFiat,
         @JsonProperty("location") GpsLocation location, @JsonProperty("distanceInMeters") int distanceInMeters,
         @JsonProperty("traderAgeMs") long traderAgeMs, @JsonProperty("tradeMedianMs") Long tradeMedianMs) {
      this.id = id;
      this.nickname = nickname;
      this.publicKey = publicKey;
      this.description = description;
      this.successfulSales = successfulSales;
      this.abortedSales = abortedSales;
      this.successfulBuys = successfulBuys;
      this.abortedBuys = abortedBuys;
      this.currency = currency;
      this.minimumFiat = minimumFiat;
      this.maximumFiat = maximumFiat;
      this.oneBtcInFiat = oneBtcInFiat;
      this.location = location;
      this.distanceInMeters = distanceInMeters;
      this.traderAgeMs = traderAgeMs;
      this.tradeMedianMs = tradeMedianMs;
   }

}
