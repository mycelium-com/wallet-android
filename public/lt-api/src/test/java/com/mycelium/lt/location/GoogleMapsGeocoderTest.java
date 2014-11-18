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

import com.google.common.base.Preconditions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class GoogleMapsGeocoderTest {


   @Test
   @Ignore
   public void testRemoteCoding() throws IOException, RemoteGeocodeException {
      GeocodeResponse response = new GoogleMapsGeocoder("en").query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   @Ignore
   public void testRemoteCodingGerman() throws IOException, RemoteGeocodeException {
      GeocodeResponse response = new GoogleMapsGeocoder("de").query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Wien, Österreich", response.results.get(0).formattedAddress);
   }

   @Test
   public void testCoding() throws IOException, RemoteGeocodeException, RemoteGeocodeException {
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/ungargasse.json"));
      GeocodeResponse response = new GoogleMapsGeocoder("en").response2Graph(stream);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   public void testPostcode() throws IOException, RemoteGeocodeException {

//      String path = "public/lt-api/src/test/resources/exampleWithPostcode.json";
//      final InputStream stream = Preconditions.checkNotNull(new FileInputStream(path));
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/exampleWithPostcode.json"));
      @SuppressWarnings("unused")
      GeocodeResponse response = new GoogleMapsGeocoder("en").response2Graph(stream);
   }
}
