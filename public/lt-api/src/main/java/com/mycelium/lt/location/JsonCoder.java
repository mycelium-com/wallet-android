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

package com.mycelium.lt.location;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.SSLException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Preconditions;

public class JsonCoder {

   final String language;

   public JsonCoder(String language) {
      this.language = language;
   }

   public GeocodeResponse query(String address, int maxresults) throws RemoteGeocodeException {

      final String encodedAddress;
      try {
         encodedAddress = URLEncoder.encode(address, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }

      String transportAgnosticUrl = "maps.googleapis.com/maps/api/geocode/json?address=" + encodedAddress
            + "&sensor=true&language=" + language;

      InputStream inputData = openStream(transportAgnosticUrl);

      GeocodeResponse res = null;
      try {
         res = response2Graph(inputData);
      } catch (RemoteGeocodeException e) {
         throw new RemoteGeocodeException(transportAgnosticUrl, e.status);
      }

      if (maxresults == -1) {
         return res;
      } else {
         return sizeLimited(res, maxresults);
      }
   }

   private InputStream openStream(String transportAgnosticUrl) {
      // Do the query using HTTPS
      String url = "https://" + transportAgnosticUrl;
      try {
         return new URL(url).openStream();
      } catch (SSLException e) {
         // Fall through and try with HTTP
      } catch (IOException e) {
         throw new RuntimeException("querying url " + url, e);
      }

      // Some older devices do not recognize the certificate of
      // maps.googleapis.com, do the query using HTTP
      url = "http://" + transportAgnosticUrl;
      try {
         return new URL(url).openStream();
      } catch (IOException e2) {
         throw new RuntimeException("querying url " + url, e2);
      }
   }

   private GeocodeResponse sizeLimited(GeocodeResponse res, int maxresults) {
      int maxSize = Math.min(res.results.size(), maxresults);
      GeocodeResponse res2 = new GeocodeResponse();
      res2.status = res.status;
      res2.results = res.results.subList(0, maxSize);
      return res2;
   }

   public GeocodeResponse getFromLocation(double latitude, double longitude) throws RemoteGeocodeException {
      String transportAgnosticUrl = "maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude
            + "&sensor=true&language=" + language;
      InputStream inputData = openStream(transportAgnosticUrl);
      return response2Graph(inputData);
   }

   public GeocodeResponse response2Graph(InputStream apply) throws RemoteGeocodeException {
      ObjectMapper mapper = new ObjectMapper(); // just need one
      mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
      // Got a Java class that data maps to nicely? If so:

      final GeocodeResponse graph;
      try {
         graph = Preconditions.checkNotNull(mapper.readValue(apply, GeocodeResponse.class));
         apply.close();
      } catch (IOException e) {
         throw new RuntimeException("error parsing response from ", e);
      }
      if (!isValidStatus(graph.status)) {
         // something is wrong, throw an error.
         throw new RemoteGeocodeException("an error occurred while querying", graph.status, graph.errorMessage);
      }
      return graph;
   }

   private boolean isValidStatus(String status) {
      return "OK".equals(status) || "ZERO_RESULTS".equals(status);
   }

}
