package com.mycelium.lt.api.params;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BtcSellPriceParameters {
   @JsonProperty
   public String currency;
   @JsonProperty
   public int fiatTraded;
   @JsonProperty
   public String priceFormulaId;
   @JsonProperty
   public double premium;

   public BtcSellPriceParameters(@JsonProperty("currency") String currency,
         @JsonProperty("fiatTraded") int fiatTraded, @JsonProperty("priceFormulaId") String priceFormulaId,
         @JsonProperty("premium") double premium) {
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
