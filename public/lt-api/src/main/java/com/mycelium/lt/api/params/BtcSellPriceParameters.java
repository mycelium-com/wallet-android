package com.mycelium.lt.api.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class BtcSellPriceParameters {
   @JsonProperty
   public Address peerId;
   @JsonProperty
   public Address ownerId;
   @JsonProperty
   public String currency;
   @JsonProperty
   public int fiatTraded;
   @JsonProperty
   public String priceFormulaId;
   @JsonProperty
   public double premium;

   public BtcSellPriceParameters(@JsonProperty("ownerId") Address ownerId, @JsonProperty("peerId") Address peerId,
         @JsonProperty("currency") String currency, @JsonProperty("fiatTraded") int fiatTraded,
         @JsonProperty("priceFormulaId") String priceFormulaId, @JsonProperty("premium") double premium) {
      this.ownerId = ownerId;
      this.peerId = peerId;
      this.currency = currency;
      this.fiatTraded = fiatTraded;
      this.priceFormulaId = priceFormulaId;
      this.premium = premium;
   }

   @SuppressWarnings("unused")
   private BtcSellPriceParameters() {
      // For Jackson
   }

}
