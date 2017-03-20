package com.mycelium.wallet.external.glidera.api.request;

import android.support.annotation.NonNull;

public class AddPhoneRequest {
   private final String phoneNumber;

   /**
    * @param phoneNumber User's new phone number. XXX-XXX-XXXX
    */
   public AddPhoneRequest(@NonNull String phoneNumber) {
      this.phoneNumber = phoneNumber;
   }

   public String getPhoneNumber() {
      return phoneNumber;
   }
}
