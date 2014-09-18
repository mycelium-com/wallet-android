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
