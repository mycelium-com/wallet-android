/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;

// Prevent Jackson t=from using getResult as this would throw an exception during serialization
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class WapiResponse<T> {
   @JsonProperty
   private int errorCode;
   private String errorMessage;
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

   public WapiResponse(int errorCode, String errorMessage, T result) {
      this.errorCode = errorCode;
      this.errorMessage = errorMessage;
      this.r = result;
   }

   public int getErrorCode() {
      return errorCode;
   }

   public String getErrorMessage() {
      return errorMessage;
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
