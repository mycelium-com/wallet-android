package com.mycelium.lt.location;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Preconditions;

import com.mycelium.lt.ErrorCallback;

public class JsonCoderTest {

   private ErrorCallback errorCallback = null; //only used for error collection

   @Test
   @Ignore
   public void testRemoteCoding() throws IOException {
      GeocodeResponse response = new JsonCoder("en", errorCallback).query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   @Ignore
   public void testRemoteCodingGerman() throws IOException {
      GeocodeResponse response = new JsonCoder("de", errorCallback).query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Wien, Österreich", response.results.get(0).formattedAddress);
   }

   @Test
   public void testCoding() throws IOException, JsonCoder.RemoteGeocodeException {
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/ungargasse.json"));
      GeocodeResponse response = new JsonCoder("en", errorCallback).response2Graph(stream);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   public void testPostcode() throws IOException, JsonCoder.RemoteGeocodeException {

//      String path = "public/lt-errorCallback/src/test/resources/exampleWithPostcode.json";
//      final InputStream stream = Preconditions.checkNotNull(new FileInputStream(path));
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/exampleWithPostcode.json"));
      @SuppressWarnings("unused")
      GeocodeResponse response = new JsonCoder("en", errorCallback).response2Graph(stream);
   }
}
