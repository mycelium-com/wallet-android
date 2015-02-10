package com.mycelium.net;


import com.squareup.okhttp.OkHttpClient;

public abstract class TorManager {
   protected int lastInitState=0;
   protected TorState stateListener;

   public void connectionOk() {
      setInitState("connection ok", 100);
   }

   public interface TorState{
      public void onStateChange(String status, int percentage);
   }

   public TorManager() {
   }

   public abstract void startClient();


   public void setStateListener(TorState stateListener){
      this.stateListener = stateListener;
   }

   protected void setInitState(String msg, int initState){
   if (stateListener != null && lastInitState != initState) {
         // post fake percentage to show progress
         stateListener.onStateChange(msg, initState);
      }
      lastInitState = initState;
   }

   public abstract void stopClient();

   public int getInitState(){
      return lastInitState;
   }

   public abstract OkHttpClient setupClient(OkHttpClient client);

   public abstract void resetInterface();
}
