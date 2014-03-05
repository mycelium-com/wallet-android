package com.mycelium.lt.api.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.model.Address;

public class LoginParameters {
   @JsonProperty
   public Address address;
   @JsonProperty
   public String signature;
   @JsonProperty
   private String gcmId;
   @JsonProperty
   private long lastTradeSessionChange;

   public LoginParameters(@JsonProperty("address") Address address, @JsonProperty("signature") String signature) {
      this.address = address;
      this.signature = signature;
   }

   public String getGcmId() {
      return gcmId;
   }

   public void setGcmId(String gcmId) {
      this.gcmId = gcmId;
   }

   public long getLastTradeSessionChange() {
      return lastTradeSessionChange;
   }

   public void setLastTradeSessionChange(long lastTradeSessionChange) {
      this.lastTradeSessionChange = lastTradeSessionChange;
   }

   @SuppressWarnings("unused")
   private LoginParameters() {
      // For Jackson
   }

}
