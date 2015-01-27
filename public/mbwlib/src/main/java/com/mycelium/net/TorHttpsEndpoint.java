package com.mycelium.net;

import com.mrd.bitlib.util.SslUtils;
import com.squareup.okhttp.OkHttpClient;

import javax.net.ssl.SSLSocketFactory;

public class TorHttpsEndpoint extends HttpEndpoint implements FeedbackEndpoint {

   private final String certificateThumbprint;
   private TorManager torManager = null;


   public TorHttpsEndpoint(String baseUrlString, String certificateThumbprint) {
      super(baseUrlString);
      this.certificateThumbprint = certificateThumbprint;
   }

   public SSLSocketFactory getSslSocketFactory(){
      return SslUtils.getSsLSocketFactory(certificateThumbprint);
   }

   @Override
   public OkHttpClient getClient() {
      OkHttpClient client = super.getClient();
      if (!(torManager instanceof TorManagerOrchid)) {
         client.setHostnameVerifier(SslUtils.HOST_NAME_VERIFIER_ACCEPT_ALL);
         client.setSslSocketFactory(this.getSslSocketFactory());
      }
      return torManager.setupClient(client);
   }


   public void setTorManager(TorManager torManager){
      this.torManager = torManager;
   }


   @Override
   public void onError(){
      torManager.resetInterface();
   }

   @Override
   public void onSuccess() {
      if (torManager != null){
         torManager.connectionOk();
      }
   }

   @Override
   public String getBaseUrl() {
      // For now tor-over-orchid only works with non-https connections,
      // which is not a big problem, because we use a hidden service, the connection end-to-end encrypted
      if (!(torManager instanceof TorManagerOrchid)) {
         return super.getBaseUrl();
      }else{
         return super.getBaseUrl().replace("https://", "http://");
      }
   }
}