package com.mycelium.net;

import com.mrd.bitlib.util.SslUtils;
import com.squareup.okhttp.OkHttpClient;

import javax.net.ssl.SSLSocketFactory;

public class HttpsEndpoint extends HttpEndpoint {
   public final String certificateThumbprint;

   public HttpsEndpoint(String baseUrlString, String certificateThumbprint) {
      super(baseUrlString);
      this.certificateThumbprint = certificateThumbprint;
   }

   public SSLSocketFactory getSslSocketFactory(){
      return SslUtils.getSsLSocketFactory(certificateThumbprint);
   }

   @Override
   public OkHttpClient getClient() {
      OkHttpClient client = super.getClient();
      client.setHostnameVerifier(SslUtils.HOST_NAME_VERIFIER_ACCEPT_ALL);
      client.setSslSocketFactory(this.getSslSocketFactory());
      return client;
   }
}
