package com.mycelium.wallet.bitid;

import android.net.Uri;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

public class BitIDSignRequest implements Serializable {


   private static final long serialVersionUID = 0L;
   private final URI uri;
   private final boolean isHttpsCallback;

   private BitIDSignRequest(Uri uri) {
      try {
         this.uri = new URI(uri.getScheme(),uri.getSchemeSpecificPart(),uri.getFragment());
         //if the parameter &u=1 exists, a http callback (instead of https) is expected
         String unsecure = uri.getQueryParameter("u");
         isHttpsCallback = (null == unsecure || !unsecure.equals("1"));
      } catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }

   public static Optional<BitIDSignRequest> parse(Uri uri) {
      String scheme = uri.getScheme();
      if (!scheme.equalsIgnoreCase("bitid")) {
         // not a bitid
         return Optional.absent();
      }
      String host = uri.getHost();
      if (null == host || host.length() < 1) return Optional.absent();
      return Optional.of(new BitIDSignRequest(uri));
   }

   public String getHost() {
      return uri.getHost();
   }

   public String getFullUri() {
      return uri.toString();
   }

   public String getHttpsUri() {
      return getUri("https");
   }

   public String getHttpUri() {
      return getUri("http");
   }

   public String getCallbackUri() {
      return isHttpsCallback ? getHttpsUri() : getHttpUri();
   }

   public boolean isSecure() {
      return isHttpsCallback;
   }

   private String getUri(String scheme) {
      try {
         return new URI(scheme, uri.getSchemeSpecificPart(), uri.getFragment()).toString();
      } catch (URISyntaxException e) {
         throw new RuntimeException(e);
      }
   }

   public JSONObject getCallbackJson(Address address, SignedMessage signature) {
      JSONObject obj = new JSONObject();
      try {
         obj.put("uri", getFullUri());
         obj.put("address", address.toString());
         obj.put("signature", signature.getBase64Signature());
      } catch (JSONException e) {
         throw new RuntimeException(e);
      }
      return obj;
   }
}
