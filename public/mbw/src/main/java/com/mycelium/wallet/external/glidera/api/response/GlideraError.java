package com.mycelium.wallet.external.glidera.api.response;

import com.google.gson.GsonBuilder;

import java.util.List;

public class GlideraError {
   public static final int ERROR_INVALID_VALUE = 1101;
   public static final int ERROR_UNKNOWN_USER = 1103;
   public static final int ERROR_INCORRECT_PIN = 2006;
   public static final int ERROR_INVALID_NONCE = 2018;
   public static final int ERROR_INVALID_AUTH1 = 2016;
   public static final int ERROR_INVALID_AUTH2 = 2017;
   public static final int ERROR_TRANSACTION_FAILED_COINS_RETURNED = 5004;
   public static final int ERROR_OCCURRED_CALL_SUPPORT = 5005;

   private Integer code;
   private String message;
   private String details;
   private List<String> invalidParameters;

   public String toString() {
      return new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(this).toString();
   }

   public Integer getCode() {
      return code;
   }

   public void setCode(Integer code) {
      this.code = code;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getDetails() {
      return details;
   }

   public void setDetails(String details) {
      this.details = details;
   }

   public List<String> getInvalidParameters() {
      return invalidParameters;
   }

   public void setInvalidParameters(List<String> invalidParameters) {
      this.invalidParameters = invalidParameters;
   }
}
