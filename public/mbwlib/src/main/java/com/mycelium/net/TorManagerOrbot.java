package com.mycelium.net;


import com.squareup.okhttp.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;

public class TorManagerOrbot extends TorManager {

   public TorManagerOrbot() {
      startClient();
   }

   @Override
   public void startClient(){
      // check if orbot is running - somehow
      setInitState("Checking Orbot", 1);
   }


   @Override
   public void stopClient(){

   }

   @Override
   public OkHttpClient setupClient(OkHttpClient client) {
      SocketAddress proxyAddress = new InetSocketAddress("127.0.0.1", 8118);
      Proxy proxy = new Proxy(Proxy.Type.HTTP, proxyAddress);
      client.setProxy(proxy);
      return client;
   }

   @Override
   public void resetInterface() {
      setInitState("Reset", 1);
   }
}
