/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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
   private final String schemeSpecificWithoutProtocol;

   private BitIDSignRequest(Uri uri) throws URISyntaxException {
      this.uri = new URI(uri.getScheme(),uri.getSchemeSpecificPart(),uri.getFragment());
      if (uri.getSchemeSpecificPart().startsWith("http")) {
         String specific = uri.getSchemeSpecificPart();
         //if the url starts with http or https, we can set isHttpsCallback based on that
         isHttpsCallback = specific.startsWith("https");
         schemeSpecificWithoutProtocol = specific.substring(specific.indexOf("//") + 2);
      } else {
         if (uri.getSchemeSpecificPart().startsWith("//")) {
            schemeSpecificWithoutProtocol = uri.getSchemeSpecificPart().substring(2);
         } else {
            schemeSpecificWithoutProtocol = uri.getSchemeSpecificPart();
         }
         //otherwise, if the parameter &u=1 exists, a http callback (instead of https) is expected
         String insecure = null;
         if (uri.isOpaque()) {
            if (schemeSpecificWithoutProtocol.contains("u=")) {
               int index = schemeSpecificWithoutProtocol.indexOf("u=");
               insecure = schemeSpecificWithoutProtocol.substring(index  + 2, index + 3);
            }
         } else {
            insecure = uri.getQueryParameter("u");
         }
         isHttpsCallback = (null == insecure || !insecure.equals("1"));
      }
   }

   public static Optional<BitIDSignRequest> parse(Uri uri) {
      String host, query;
      String scheme = uri.getScheme();
      if (!scheme.equalsIgnoreCase("bitid")) {
         // not a bitid
         return Optional.absent();
      }

      if (uri.isOpaque()) {
         String url = uri.getSchemeSpecificPart();
         if (url.startsWith("http")) {
            //remove the protocol
            if (!url.contains("//")) return Optional.absent();
            url = url.substring(url.indexOf("//") + 2);
         }
         if (!url.contains("?")) return Optional.absent();
         host = url.substring(0, url.indexOf('?'));
         query = url.substring(url.indexOf('?') + 1);
      } else {
         host = uri.getHost();
         query = uri.getQuery();
      }

      if (null == host || host.length() < 1) return Optional.absent();
      if (null == query || query.length() < 1) return Optional.absent();

      try {
         return Optional.of(new BitIDSignRequest(uri));
      } catch (URISyntaxException e) {
         return Optional.absent();
      }
   }

   public String getHost() {
      if (uri.isOpaque()) {
         if (schemeSpecificWithoutProtocol.contains("/")) {
            return schemeSpecificWithoutProtocol.substring(0, schemeSpecificWithoutProtocol.indexOf("/"));
         }
         if (schemeSpecificWithoutProtocol.contains("?")) {
            return schemeSpecificWithoutProtocol.substring(0, schemeSpecificWithoutProtocol.indexOf("?"));
         }
         return schemeSpecificWithoutProtocol;
      }
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
         return new URI(scheme, "//" + schemeSpecificWithoutProtocol, uri.getFragment()).toString();
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

   public String getIdUri() {
      String uriString = getCallbackUri();
      if (uriString.contains("?")) return uriString.substring(0, uriString.indexOf("?"));
      return uriString;
   }
}
