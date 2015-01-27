package com.mycelium.net;


import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.Network;
import com.subgraph.orchid.TorClient;
import com.subgraph.orchid.TorConfig;
import com.subgraph.orchid.TorInitializationListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;


public class TorManagerOrchid extends TorManager implements TorInitializationListener {
   private TorClient torClient;
   private Object torClientLock = new Object();
   private TorConfig config;

   public TorManagerOrchid(TorConfig config) {
      this.config = config;
      initClient();
      new Thread(new Runnable() {
         @Override
         public void run() {
            TorManagerOrchid.this.startClient();
         }
      }).run();
   }

   private void initClient() {
      synchronized (torClientLock){
         torClient = new TorClient(this.config, null);
         torClient.addInitializationListener(this);
      }
   }


   @Override
   public synchronized void startClient(){
      if (torClient == null) {
         initClient();
      }

      setInitState("Starting", 1);
      torClient.start();
   }


   @Override
   public synchronized void stopClient(){
         torClient.stop();
         setInitState("Stop", 0);
         torClient = null;
   }

   @Override
   public OkHttpClient setupClient(OkHttpClient client) {
      // configure the Look-Up algorithm to use our dumb-lookup, which just
      // returns the hostname without looking it up
      Internal.instance.setNetwork(client,new Network() {
         @Override
         public InetAddress[] resolveInetAddresses(String host) throws UnknownHostException {
            return new InetAddress[]{
                  // hack to get the literal hostname of the hidden service to the tor-socket,
                  // without trying to resolve it via normal DNS
                  HiddenServiceInetAddress.getInstance(host)
            };
         }
      });

      try {
         this.getTorClient().waitUntilReady(120*1000);
         client.setSocketFactory(this.getTorClient().getSocketFactory());
      } catch (InterruptedException e) {
         //
      } catch (TimeoutException e) {
         //
      }
      return client;
   }

   @Override
   public void resetInterface() {
      this.stopClient();
   }

   public synchronized TorClient getTorClient() {
      if (torClient == null) {
         startClient();
      }
      return torClient;
   }


   @Override
   public synchronized void initializationProgress(String message, int percent) {
      lastInitState=percent;
      if (stateListener != null){
         stateListener.onStateChange(message, percent);
      }
   }

   @Override
   public void initializationCompleted() {

   }

}
