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

package com.mrd.mbwapi.util;

import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.mrd.bitlib.util.HexUtils;

/**
 * SSL Utilities.
 */
public class SslUtils {

   private static final Map<String, SSLSocketFactory> _sslSocketFactories = new HashMap<String, SSLSocketFactory>();

   private static final HostnameVerifier HOST_NAME_VERIFIER;

   private static synchronized SSLSocketFactory getSsLSocketFactory(String certificateThumbprint) {
      SSLSocketFactory factory = _sslSocketFactories.get(certificateThumbprint);
      if (factory == null) {
         factory = createSslSocketFactory(certificateThumbprint);
         _sslSocketFactories.put(certificateThumbprint, factory);
      }
      return factory;
   }

   private static SSLSocketFactory createSslSocketFactory(final String certificateThumbprint) {
      // Make a trust manager that trusts a pinned server certificate and
      // nothing else
      TrustManager[] trustOneCert = new TrustManager[] { new X509TrustManager() {
         public X509Certificate[] getAcceptedIssuers() {
            return null;
         }

         public void checkClientTrusted(X509Certificate[] certs, String authType)
               throws java.security.cert.CertificateException {
            // We do not use a client side certificate
            throw new CertificateException();
         }

         public void checkServerTrusted(X509Certificate[] certs, String authType)
               throws java.security.cert.CertificateException {
            if (certs == null || certs.length == 0) {
               throw new CertificateException();
            }
            for (X509Certificate certificate : certs) {
               String sslThumbprint = generateCertificateThumbprint(certificate);
               if (certificateThumbprint.equalsIgnoreCase(sslThumbprint)) {
                  return;
               }
            }
            throw new CertificateException();
         }
      } };

      // Create an SSL socket factory which trusts the pinned server certificate
      try {
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, trustOneCert, null);
         return sc.getSocketFactory();
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (KeyManagementException e) {
         throw new RuntimeException(e);
      }
   }

   static {

      // Used for disabling host name verification. This is safe because we
      // trust the BCCAPI server certificate explicitly
      HOST_NAME_VERIFIER = new HostnameVerifier() {
         @Override
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      };

   }

   /**
    * Makes an URL connection to accept a server-side certificate with specific
    * thumbprint and ignore host name verification. This is useful and safe if
    * you have a client with a hard coded well-known certificate
    * 
    * @param connection
    *           The connection to configure
    * @param serverThumbprint
    *           The X509 thumbprint of the server side certificate
    */
   public static void configureTrustedCertificate(URLConnection connection, final String serverThumbprint) {
      if (!(connection instanceof HttpsURLConnection)) {
         return;
      }

      HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) connection;

      if (httpsUrlConnection.getHostnameVerifier() != HOST_NAME_VERIFIER) {
         httpsUrlConnection.setHostnameVerifier(HOST_NAME_VERIFIER);
      }
      SSLSocketFactory sslSocketFactory = getSsLSocketFactory(serverThumbprint);
      if (httpsUrlConnection.getSSLSocketFactory() != sslSocketFactory) {
         httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
      }
   }

   /**
    * Generates an SSL thumbprint from a certificate
    * 
    * @param certificate
    *           The certificate
    * @return The thumbprint of the certificate
    */
   private static String generateCertificateThumbprint(Certificate certificate) {
      try {
         MessageDigest md;
         try {
            md = MessageDigest.getInstance("SHA-1");
         } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
         }
         byte[] encoded;

         try {
            encoded = certificate.getEncoded();
         } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
         }
         return HexUtils.toHex(md.digest(encoded), ":");
      } catch (Exception e) {
         return null;
      }
   }

}
