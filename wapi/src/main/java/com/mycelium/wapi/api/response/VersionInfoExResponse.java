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

package com.mycelium.wapi.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.net.URI;
import java.util.List;


public class VersionInfoExResponse implements Serializable {

   @JsonProperty
   public String versionNumber;

   @JsonProperty
   public String versionMessage;

   @JsonProperty
   public URI directDownload;

   @JsonProperty
   public List<FeatureWarning> featureWarnings;

   public VersionInfoExResponse(@JsonProperty("versionNumber") String versionNumber,
                                @JsonProperty("versionMessage") String versionMessage,
                                @JsonProperty("directDownload") URI directDownload,
                                @JsonProperty("featureWarnings") List<FeatureWarning> featureWarnings
                                ) {
      this.versionNumber = versionNumber;
      this.versionMessage = versionMessage;
      this.directDownload = directDownload;
      this.featureWarnings = featureWarnings;
   }

}
