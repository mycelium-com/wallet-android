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
   public void testRemoteCoding() throws IOException {
      GeocodeResponse response = new JsonCoder().query("Ungargasse 6 1030 Wien", -1);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }

   @Test
   public void testCoding() throws IOException {
      final InputStream stream = Preconditions.checkNotNull(getClass().getResourceAsStream("/ungargasse.json"));
      GeocodeResponse response = new JsonCoder().response2Graph(stream);
      assertEquals("Ungargasse 6, 1030 Vienna, Austria", response.results.get(0).formattedAddress);
   }
}
