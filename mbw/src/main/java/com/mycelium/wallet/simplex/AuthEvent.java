package com.mycelium.wallet.simplex;

import android.os.Handler;

/**
 * Created by tomb on 11/23/16.
 */

public class AuthEvent {

   public Boolean isSuccess;

   public String errorMessage;

   public Handler activityHandler;

   public LvlResponse responseData;

   public class LvlResponse {

      public int responseCode;

      public String signedData;

      public String signature;
   }
}
