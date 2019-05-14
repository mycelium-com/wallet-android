/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wallet.lt;

import com.mycelium.lt.api.LtApiClient;
import com.mycelium.lt.api.LtApiException;
import com.mycelium.lt.api.LtResponse;
import com.mycelium.lt.api.model.GeocoderSearchResults;
import com.mycelium.lt.api.model.LtSession;
import com.mycelium.lt.location.GeocodeResponse;
import com.mycelium.lt.location.Geocoder;
import com.mycelium.lt.location.RemoteGeocodeException;

public class BackendGeocoder extends Geocoder {
   private final LocalTraderManager ltManager;

   public BackendGeocoder(LocalTraderManager ltManager) {
      this.ltManager = ltManager;
   }

   @Override
   public GeocodeResponse query(String address, int maxResults) {
      LtResponse<GeocoderSearchResults> geocodeResult = ltManager.getApi().searchGeocoder(ltManager.getSession().id, address, maxResults);

      try {
         return geocodeResult.getResult().results;
      } catch (LtApiException e) {
         throw new RuntimeException("Backend Exception; " + e.getMessage());
      }
   }

   @Override
   public GeocodeResponse getFromLocation(double latitude, double longitude) throws RemoteGeocodeException {
      LtSession session = ltManager.getSession();
      if (session != null) {
         LtResponse<GeocoderSearchResults> geocodeResult = ltManager.getApi().reverseGeocoder(session.id, latitude, longitude);
         try {
            return geocodeResult.getResult().results;
         } catch (LtApiException e) {
            throw new RemoteGeocodeException(((LtApiClient) ltManager.getApi()).getUrl(), "Backend Exception; " + e.getMessage());
         }
      } else {
         // return an empty response, if our local session does not exist
         return new GeocodeResponse();
      }
   }
}
