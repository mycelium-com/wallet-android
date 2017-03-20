package com.mycelium.wapi.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.net.URI;

public class FeatureWarning implements Serializable {
   @JsonProperty
   public Feature feature;

   @JsonProperty
   public WarningKind warningKind;

   @JsonProperty
   public String warningMessage;

   @JsonProperty
   public URI link;


   public FeatureWarning(@JsonProperty("feature") Feature feature,
                         @JsonProperty("warningKind") WarningKind warningKind,
                         @JsonProperty("warningMessage") String warningMessage,
                         @JsonProperty("link") URI link) {
      this.feature = feature;
      this.warningKind = warningKind;
      this.warningMessage = warningMessage;
      this.link = link;
   }
}
