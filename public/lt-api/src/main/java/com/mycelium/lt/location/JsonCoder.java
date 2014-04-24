/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.lt.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

public class JsonCoder {

   final String language;

   public JsonCoder(String language) {
      this.language = language;
   }

   public GeocodeResponse query(String address, int maxresults) throws RemoteGeocodeException {
      final InputStream inputData;
      final String encodedAddress;
      try {
         encodedAddress = URLEncoder.encode(address, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      }
      String url = "https://maps.googleapis.com/maps/api/geocode/json?address=" + encodedAddress + "&sensor=true&language=" + language;
      try {
         inputData = new URL(url).openStream();
      } catch (IOException e) {
         throw new RuntimeException("querying url " + url, e);
      }
      GeocodeResponse res = null;
      try {
         res = response2Graph(inputData);
      } catch (RemoteGeocodeException e) {
         throw new RemoteGeocodeException(url, e.status);
      }

      if (maxresults == -1) {
         return res;
      } else {
         return sizeLimited(res, maxresults);
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
      final InputStream inputData;
      String url = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude + "," + longitude + "&sensor=true&language=" + language;
      try {
         inputData = new URL(url).openStream();
      } catch (IOException e) {
         throw new RuntimeException("querying url " + url, e);
      }
      return response2Graph(inputData);
/*
         errorCallback.collectError(e, url, latitude + "," + longitude);
         GeocodeResponse dummy = new GeocodeResponse();
         dummy.results = ImmutableList.of();
         return dummy;
*/

   }


   public GeocodeResponse response2Graph(InputStream apply) throws RemoteGeocodeException {
      ObjectMapper mapper = new ObjectMapper(); // just need one
      mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
      // Got a Java class that data maps to nicely? If so:

      final GeocodeResponse graph;
      try {
         graph = Preconditions.checkNotNull(mapper.readValue(apply, GeocodeResponse.class));
      } catch (IOException e) {
         throw new RuntimeException("error parsing response from ", e);
      }
      if (!isValidStatus(graph.status)) {
         //something is wrong, throw an error.
         throw new RemoteGeocodeException("an error occurred while querying", graph.status, graph.errorMessage);
      }
      return graph;
   }

   private boolean isValidStatus(String status) {
      return "OK".equals(status) || "ZERO_RESULTS".equals(status);
   }


}
