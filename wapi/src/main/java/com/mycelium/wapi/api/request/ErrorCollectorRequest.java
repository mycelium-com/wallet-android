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

package com.mycelium.wapi.api.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Throwables;
import com.mycelium.wapi.api.lib.ErrorMetaData;

import java.io.Serializable;

public class ErrorCollectorRequest implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public String error;

   @JsonProperty
   public String appVersion;


   @JsonCreator
   public ErrorCollectorRequest(@JsonProperty("error") String error,
                                @JsonProperty("appVersion") String appVersion) {
      // Json constructor
      this.error = error;
      this.appVersion = appVersion;
   }


   public ErrorCollectorRequest(Throwable error, String appVersion, ErrorMetaData metaData) {

      //does not make much sense to have actual stacktraces here because we are traversing VMs
      this.error = Throwables.getStackTraceAsString(error) + "\n" + metaData.toString();
      this.appVersion = appVersion;
   }

   @Override
   public String toString() {
      return "ErrorCollectionRequest{" +
            "error='" + error + '\'' +
            ", version='" + appVersion + '\'' +
            '}';
   }
}
