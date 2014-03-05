package com.mycelium.lt.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

// Prevent Jackson t=from using getResult as this would throw an exception during serialization
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class LtResponse<T> {
   @JsonProperty
   private int errorCode;
   @JsonProperty
   private T r;

   public LtResponse(T result) {
      this.errorCode = LtApi.ERROR_CODE_SUCCESS;
      this.r = result;
   }

   public LtResponse(int errorCode, T result) {
      this.errorCode = errorCode;
      this.r = result;
   }

   public int getErrorCode() {
      return errorCode;
   }

   public T getResult() throws LtApiException {
      if (errorCode != LtApi.ERROR_CODE_SUCCESS) {
         throw new LtApiException(errorCode);
      }
      return r;
   }

   public LtResponse() {
      // For Jackson
   }
}
