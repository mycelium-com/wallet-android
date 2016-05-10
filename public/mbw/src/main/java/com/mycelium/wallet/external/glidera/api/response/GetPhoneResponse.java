package com.mycelium.wallet.external.glidera.api.response;

public class GetPhoneResponse extends GlideraResponse {
   private String phoneNumber;

   public String getPhoneNumber() {
      return phoneNumber;
   }

   public void setPhoneNumber(String phoneNumber) {
      this.phoneNumber = phoneNumber;
   }
}
