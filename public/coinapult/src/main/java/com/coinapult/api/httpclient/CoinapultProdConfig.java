package com.coinapult.api.httpclient;

import java.security.PublicKey;

/**
 * Created by Andreas on 30.03.2015.
 */
public class CoinapultProdConfig extends CoinapultConfig {
   static final String COINAPULT_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
         + "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEWp9wd4EuLhIZNaoUgZxQztSjrbqgTT0w\n"
         + "LBq8RwigNE6nOOXFEoGCjGfekugjrHWHUi8ms7bcfrowpaJKqMfZXg==\n"
         + "-----END PUBLIC KEY-----";

   private static final String BASE_URL = "https://api.coinapult.com";


   @Override
   public PublicKey getPubKey() {
      return importPublicFromPEM((COINAPULT_PUBLIC_KEY));
   }

   @Override
   public String getBaseUrl() {
      return BASE_URL;
   }

}
