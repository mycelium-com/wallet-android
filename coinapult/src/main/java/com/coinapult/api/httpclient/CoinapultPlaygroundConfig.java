package com.coinapult.api.httpclient;

import java.security.PublicKey;

/**
 * Created by Andreas on 30.03.2015.
 */
public class CoinapultPlaygroundConfig extends CoinapultConfig {
   @Override
   public PublicKey getPubKey() {
return importPublicFromPEM("-----BEGIN PUBLIC KEY-----\n" +
      "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEIizVLeWox/K7p/fHcD9AjLqwVd9nOA6C\n" +
      "IOElQDJsMnPjFnWFB2P2+XoFbqguye4K8mJ/yOKo8TnLB0uIDUslWA==\n" +
      "-----END PUBLIC KEY-----");
   }

   @Override
   public String getBaseUrl() {
      return "https://playground.coinapult.com";
   }
}
