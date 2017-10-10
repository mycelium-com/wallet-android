package com.mycelium.net;

import com.mrd.bitlib.util.SslUtils;
import com.squareup.okhttp.OkHttpClient;

import java.rmi.RemoteException;

import javax.net.ssl.SSLSocketFactory;

public class TorHttpsEndpoint extends HttpsEndpoint implements FeedbackEndpoint {

   private TorManager torManager = null;


   public TorHttpsEndpoint(String baseUrlString, String certificateThumbprint) {
      super(baseUrlString, certificateThumbprint);
   }

   public SSLSocketFactory getSslSocketFactory(){
      return SslUtils.getSsLSocketFactory(certificateThumbprint);
   }

   @Override
   public OkHttpClient getClient() {
      OkHttpClient client = super.getClient();
      if(torManager == null) {
         throw new RuntimeException("tor manager not found");
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
      return super.getBaseUrl();
   }
}