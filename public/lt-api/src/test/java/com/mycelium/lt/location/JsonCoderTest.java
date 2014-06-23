package com.mycelium.lt.location;

import com.google.common.base.Preconditions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class JsonCoderTest {


   @Test
   @Ignore
   public void testRemoteCoding() throws IOException, RemoteGeocodeException {
      GeocodeResponse response = new JsonCoder("en").query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   @Ignore
   public void testRemoteCodingGerman() throws IOException, RemoteGeocodeException {
      GeocodeResponse response = new JsonCoder("de").query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Wien, Österreich", response.results.get(0).formattedAddress);
   }

   @Test
   public void testCoding() throws IOException, RemoteGeocodeException, RemoteGeocodeException {
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/ungargasse.json"));
      GeocodeResponse response = new JsonCoder("en").response2Graph(stream);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   public void testPostcode() throws IOException, RemoteGeocodeException {

//      String path = "public/lt-api/src/test/resources/exampleWithPostcode.json";
//      final InputStream stream = Preconditions.checkNotNull(new FileInputStream(path));
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/exampleWithPostcode.json"));
      @SuppressWarnings("unused")
      GeocodeResponse response = new JsonCoder("en").response2Graph(stream);
   }
}
