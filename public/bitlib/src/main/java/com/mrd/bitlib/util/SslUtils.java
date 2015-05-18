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

package com.mrd.bitlib.util;

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


/**
 * SSL Utilities.
 */
public class SslUtils {

   private static final Map<String, SSLSocketFactory> _sslSocketFactories = new HashMap<String, SSLSocketFactory>();

   public static final HostnameVerifier HOST_NAME_VERIFIER_ACCEPT_ALL;
   public static final  SSLSocketFactory SSL_SOCKET_FACTORY_ACCEPT_ALL;

   public static synchronized SSLSocketFactory getSsLSocketFactory(String certificateThumbprint) {
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
      // trust the MWAPI server certificate explicitly
      HOST_NAME_VERIFIER_ACCEPT_ALL = new HostnameVerifier() {
         @Override
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      };

      //not used for our servers - sometimes needed after user confirmed to contact external services besides cert errors
      TrustManager[] trustOneCert = new TrustManager[] { new X509TrustManager() {
         public X509Certificate[] getAcceptedIssuers() {
            return null;
         }
         public void checkClientTrusted(X509Certificate[] certs, String authType) {
            //everything is fine
         }
         public void checkServerTrusted(X509Certificate[] certs, String authType) {
            //everything is fine
         }
      } };

      try {
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, trustOneCert, null);
         SSL_SOCKET_FACTORY_ACCEPT_ALL = sc.getSocketFactory();
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (KeyManagementException e) {
         throw new RuntimeException(e);
      }
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

      if (httpsUrlConnection.getHostnameVerifier() != HOST_NAME_VERIFIER_ACCEPT_ALL) {
         httpsUrlConnection.setHostnameVerifier(HOST_NAME_VERIFIER_ACCEPT_ALL);
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
