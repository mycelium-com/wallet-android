package com.mycelium.wallet.external.glidera.api.response;

public class OAuth1Response extends GlideraResponse {
   private String access_key;
   private String secret;

   public String getAccess_key() {
      return access_key;
   }

   public void setAccess_key(String access_key) {
      this.access_key = access_key;
   }

   public String getSecret() {
      return secret;
   }

   public void setSecret(String secret) {
      this.secret = secret;
   }

}
