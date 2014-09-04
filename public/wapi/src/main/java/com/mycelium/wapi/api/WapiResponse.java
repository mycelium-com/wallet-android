package com.mycelium.wapi.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

// Prevent Jackson t=from using getResult as this would throw an exception during serialization
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class WapiResponse<T> {
   @JsonProperty
   private int errorCode;
   @JsonProperty
   private T r;

   public WapiResponse(T result) {
      this.errorCode = Wapi.ERROR_CODE_SUCCESS;
      this.r = result;
   }

   public WapiResponse(int errorCode, T result) {
      this.errorCode = errorCode;
      this.r = result;
   }

   public int getErrorCode() {
      return errorCode;
   }

   public T getResult() throws WapiException {
      if (errorCode != Wapi.ERROR_CODE_SUCCESS) {
         throw new WapiException(errorCode);
      }
      return r;
   }

   public WapiResponse() {
      // For Jackson
   }
}
