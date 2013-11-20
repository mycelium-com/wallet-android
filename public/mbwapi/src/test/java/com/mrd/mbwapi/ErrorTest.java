package com.mrd.mbwapi;

import org.junit.Ignore;
import org.junit.Test;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl;

@Ignore
public class ErrorTest {

   @Test
   public void errorCollect() throws ApiException {
      MyceliumWalletApiImpl.HttpEndpoint[] endpoints = { new MyceliumWalletApiImpl.HttpEndpoint(
            "http://localhost:8080/mws") };
      MyceliumWalletApiImpl toTest = new MyceliumWalletApiImpl(endpoints, NetworkParameters.productionNetwork);

      /*
       * ErrorCollectionResponse response = toTest.collectError(new
       * RuntimeException("test!"), "0.0.0 - integration Test");
       * ErrorCollectionResponse response2 = toTest.collectError(new
       * RuntimeException(String.valueOf(new char[12000])),
       * " large error 0.0.0 - integration Test");
       */
      try {
         recurse();
      } catch (StackOverflowError err) {
         toTest.collectError(err, "junit");
      }
   }

   // Not supported by eclipse: @SuppressWarnings("InfiniteRecursion")
   private void recurse() {
      recurse();
   }
}
