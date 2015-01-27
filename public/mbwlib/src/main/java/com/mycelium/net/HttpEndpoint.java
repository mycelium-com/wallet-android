package com.mycelium.net;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpEndpoint {
   private final String baseUrlString;

   public HttpEndpoint(String baseUrlString) {
      this.baseUrlString = baseUrlString;
   }

   @Override
   public String toString() {
      return getBaseUrl();
   }

   public String getBaseUrl(){
      return baseUrlString;
   }

   public URI getUri(String basePath, String function) throws IOException {
      try {
         URI uri = new URI(this.getBaseUrl() + basePath + '/' + function);
         return uri;
      } catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }

   public URI getUri(String function) throws IOException {
      try {
         URI uri = new URI(this.getBaseUrl() + '/' + function);
         return uri;
      } catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }

   public OkHttpClient getClient(){
      return new OkHttpClient();
   }

}
