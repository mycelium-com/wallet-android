package com.mycelium.wallet.service;

import java.io.Serializable;

import android.content.Context;

public abstract class ServiceTask<T2 extends Serializable> implements Serializable {
   private static final long serialVersionUID = 1L;

   private T2 _result;
   private Exception _e;

   public T2 getResult() throws Exception {
      if (_e != null) {
         throw _e;
      }
      return _result;
   }

   public void run(Context context) {
      try {
         _result = doTask(context);
      } catch (Exception e) {
         _e = e;
      }
   }

   protected abstract T2 doTask(Context context) throws Exception;
   protected abstract void terminate();
   protected abstract ServiceTaskStatus getStatus();
}
