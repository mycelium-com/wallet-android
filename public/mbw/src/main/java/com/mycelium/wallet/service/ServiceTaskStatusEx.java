package com.mycelium.wallet.service;

public class ServiceTaskStatusEx extends ServiceTaskStatus {
   private static final long serialVersionUID = 1L;

   public enum State {
      NOTRUNNING, STARTING, RUNNING, FINISHED
   };

   public State state;

   public ServiceTaskStatusEx(String statusMessage, Double progress, State state) {
      super(statusMessage, progress);
      this.state = state;
   }
}
