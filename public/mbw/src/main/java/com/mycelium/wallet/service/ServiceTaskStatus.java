package com.mycelium.wallet.service;

import java.io.Serializable;

public class ServiceTaskStatus implements Serializable {
   private static final long serialVersionUID = 1L;

   public String statusMessage;
   public Double progress;

   public ServiceTaskStatus(String statusMessage, Double progress) {
      this.statusMessage = statusMessage;
      this.progress = progress;
   }
}
