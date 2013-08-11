package com.mrd.mbwapi.impl.shorten;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.common.base.Optional;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.StringUtils;
import com.mrd.mbwapi.api.AddressShort;

public class BtcTo implements AddressShort {

   @Override
   public Optional<Address> query(String input) {
      try {
         URL query = new URL("http://btc.to/" + input);
         HttpURLConnection connection = (HttpURLConnection) query.openConnection();
         connection.setRequestMethod("GET");
         connection.setReadTimeout(TIMEOUT);
         connection.connect();
         int status = connection.getResponseCode();
         if (status != 200) {
            return null;
         }
         InputStreamReader reader = new InputStreamReader(connection.getInputStream());
         String s = StringUtils.readFully(reader);
         Address address = Address.fromString(s);
         if (address != null) {
            return Optional.of(address);
         }
      } catch (MalformedURLException ignored) {
         //most likely user entered something stupid
      } catch (IOException e) {
         //if something went wrong
         throw new RuntimeException(e);
      }
      return Optional.absent();
   }
}
