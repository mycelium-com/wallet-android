package com.mycelium.wallet.external.glidera.api.response;

public class TwoFactorResponse extends GlideraResponse {
   private Mode mode;
   private String status;

   public Mode getMode() {
      return mode;
   }

   public void setMode(Mode mode) {
      this.mode = mode;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public enum Mode {
      SMS, AUTHENTICATR, PIN, NONE;
   }
}
