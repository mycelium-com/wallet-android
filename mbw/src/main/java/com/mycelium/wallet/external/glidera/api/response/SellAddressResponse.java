package com.mycelium.wallet.external.glidera.api.response;

public class SellAddressResponse extends GlideraResponse {
   private String sellAddress;

   public String getSellAddress() {
      return sellAddress;
   }

   public void setSellAddress(String sellAddress) {
      this.sellAddress = sellAddress;
   }
}
