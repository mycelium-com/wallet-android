package com.mycelium.lt.api.model;

import java.io.Serializable;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;

public class TraderInfo extends PublicTraderInfo implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final double localTraderPremium;
   @JsonProperty
   public final long totalBtcBought;
   @JsonProperty
   public final long totalBtcSold;
   @JsonProperty
   public final LinkedList<TradeSession> activeTradeSesions;

   public TraderInfo(@JsonProperty("nickname") String nickname, @JsonProperty("address") Address address, @JsonProperty("publicKey") PublicKey publicKey,
         @JsonProperty("traderAgeMs") long traderAgeMs, @JsonProperty("lastChange") long lastChange,
         @JsonProperty("localTraderPremium") double localTraderPremium,
         @JsonProperty("successfulSales") int successfulSales, @JsonProperty("successfulBuys") int successfulBuys,
         @JsonProperty("abortedSales") int abortedSales, @JsonProperty("abortedBuys") int abortedBuys,
         @JsonProperty("totalBtcBought") long totalBtcBought, @JsonProperty("totalBtcSold") long totalBtcSold,
         @JsonProperty("tradeMedianMs") Long tradeMedianMs,
         @JsonProperty("activeTradeSesions") LinkedList<TradeSession> activeTradeSesions) {
      super(nickname, address, publicKey, traderAgeMs, lastChange, successfulSales, successfulBuys, abortedSales, abortedBuys,
            tradeMedianMs);
      this.localTraderPremium = localTraderPremium;
      this.totalBtcBought = totalBtcBought;
      this.totalBtcSold = totalBtcSold;
      this.activeTradeSesions = activeTradeSesions;
   }

}
