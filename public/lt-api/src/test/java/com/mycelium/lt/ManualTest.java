package com.mycelium.lt;

import org.junit.Ignore;
import org.junit.Test;

import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiClient;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.LtResponse;
import com.mycelium.lt.api.model.AdType;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.api.model.LtSession;
import com.mycelium.lt.api.params.SearchParameters;

public class ManualTest {

   private static final LtApiClient.Logger LOGGER = new LtApiClient.Logger() {
      @Override
      public void logError(String message, Exception e) {
         System.out.println(message);
         e.printStackTrace();
      }

      @Override
      public void logError(String message) {
         System.out.println(message);
      }
   };

   @Test
   @Ignore
   public void testManualConnect() throws LtApiException {
      LtApiClient.HttpEndpoint testnetLocalTraderEndpoint = new LtApiClient.HttpEndpoint(
            "http://192.168.178.53:8098/trade/");

      LtApiClient client = new LtApiClient(testnetLocalTraderEndpoint, LOGGER);
      LtResponse<LtSession> session = client.createSession(LtApi.VERSION, "en", "BTC");
      client.sellOrderSearch(session.getResult().id, new SearchParameters(new GpsLocation(0, 0, "dummy"), 2,
            AdType.SELL_BTC));
   }
}
