package com.mycelium.lt.api.params;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TradeChangeParameters implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public UUID tradeSessionId;
   @JsonProperty
   public String priceFormulaId;
   @JsonProperty
   public double premium;

   public TradeChangeParameters(@JsonProperty("tradeSessionId") UUID tradeSessionId,
         @JsonProperty("priceFormulaId") String priceFormulaId, @JsonProperty("premium") double premium) {
      this.tradeSessionId = tradeSessionId;
      this.priceFormulaId = priceFormulaId;
      this.premium = premium;
   }

   @SuppressWarnings("unused")
   private TradeChangeParameters() {
      // For Jackson
   }

}
