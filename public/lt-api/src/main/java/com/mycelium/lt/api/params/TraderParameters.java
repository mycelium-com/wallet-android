package com.mycelium.lt.api.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;

//todo rename TadeParameters / TraderParameters confusing
public class TraderParameters {
   @JsonProperty
   public String nickname;
   @JsonProperty
   public Address address;
   @JsonProperty
   public PublicKey publicKey;
   @JsonProperty
   public String sigSessionId;

   public TraderParameters(@JsonProperty("nickname") String nickname, @JsonProperty("address") Address address, @JsonProperty("publicKey") PublicKey publicKey,
         @JsonProperty("sigSessionId") String sigSessionId) {
      this.nickname = nickname;
      this.address = address;
      this.publicKey = publicKey;
      this.sigSessionId = sigSessionId;
   }

   @SuppressWarnings("unused")
   private TraderParameters() {
      // For Jackson
   }
}
