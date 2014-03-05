package com.mycelium.lt.api.model;

import java.io.Serializable;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class TraderInfo implements Serializable {
   private static final long serialVersionUID = 1L;
   
   @JsonProperty
   public String nickname;
   @JsonProperty
   public final Address address;
   @JsonProperty
   public final long traderAgeMs;
   @JsonProperty
   public final long lastChange;
   @JsonProperty
   public final double localTraderPremium;
   @JsonProperty
   public final int successfulSales;
   @JsonProperty
   public final int successfulBuys;
   @JsonProperty
   public final int abortedSales;
   @JsonProperty
   public final int abortedBuys;
   @JsonProperty
   public final long totalBtcBought;
   @JsonProperty
   public final long totalBtcSold;
   @JsonProperty
   public final LinkedList<TradeSessionStatus> activeTradeSesions;

   public TraderInfo(@JsonProperty("nickname") String nickname, @JsonProperty("address") Address address,
         @JsonProperty("traderAgeMs") long traderAgeMs, @JsonProperty("lastChange") long lastChange,
         @JsonProperty("localTraderPremium") double localTraderPremium,
         @JsonProperty("successfulSales") int successfulSales, @JsonProperty("successfulBuys") int successfulBuys,
         @JsonProperty("abortedSales") int abortedSales, @JsonProperty("abortedBuys") int abortedBuys,
         @JsonProperty("totalBtcBought") long totalBtcBought, @JsonProperty("totalBtcSold") long totalBtcSold,
         @JsonProperty("activeTradeSesions") LinkedList<TradeSessionStatus> activeTradeSesions) {
      this.nickname = nickname;
      this.address = address;
      this.traderAgeMs = traderAgeMs;
      this.lastChange = lastChange;
      this.localTraderPremium = localTraderPremium;
      this.successfulSales = successfulSales;
      this.successfulBuys = successfulBuys;
      this.abortedSales = abortedSales;
      this.abortedBuys = abortedBuys;
      this.totalBtcBought = totalBtcBought;
      this.totalBtcSold = totalBtcSold;
      this.activeTradeSesions = activeTradeSesions;
   }

}
