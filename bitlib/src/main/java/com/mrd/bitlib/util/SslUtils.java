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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * SSL Utilities.
 */
public class SslUtils {
   private static final Map<String, SSLSocketFactory> _sslSocketFactories = new HashMap<String, SSLSocketFactory>();

   public static final HostnameVerifier HOST_NAME_VERIFIER_ACCEPT_ALL;
   public static final SSLSocketFactory SSL_SOCKET_FACTORY_ACCEPT_ALL;

   // ISRG Root X1 — the Let's Encrypt root CA. It is absent from the system trust
   // store on Android < 7.1.1 (API 25), so chains served by Let's Encrypt fail with
   // "Trust anchor for certification path not found" on those devices. We bundle it
   // and trust it in addition to (not instead of) the system CAs.
   private static final String ISRG_ROOT_X1_PEM =
         "-----BEGIN CERTIFICATE-----\n" +
         "MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw\n" +
         "TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh\n" +
         "cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4\n" +
         "WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu\n" +
         "ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY\n" +
         "MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc\n" +
         "h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+\n" +
         "0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U\n" +
         "A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW\n" +
         "T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH\n" +
         "B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC\n" +
         "B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv\n" +
         "KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn\n" +
         "OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn\n" +
         "jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw\n" +
         "qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI\n" +
         "rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV\n" +
         "HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq\n" +
         "hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL\n" +
         "ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ\n" +
         "3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK\n" +
         "NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5\n" +
         "ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur\n" +
         "TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC\n" +
         "jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc\n" +
         "oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq\n" +
         "4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA\n" +
         "mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d\n" +
         "emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=\n" +
         "-----END CERTIFICATE-----\n";

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

            // none of the provided server certs match the expected one - report and error and
            // reject any communication with that server
            String providedCerts="";
            for (X509Certificate certificate : certs) {
               providedCerts += " " + generateCertificateThumbprint(certificate);
            }
            throw new CertificateException("Not expected certificate: " + providedCerts + " but expected: " + certificateThumbprint);
         }
      } };

      // Create an SSL socket factory which trusts the pinned server certificate
      try {
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, trustOneCert, null);
         return sc.getSocketFactory();
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Returns an {@link SSLSocketFactory} that trusts the platform's default CAs plus the
    * bundled ISRG Root X1 (Let's Encrypt) root. Android added ISRG Root X1 to its system
    * trust store only in 7.1.1 (API 25), so on Android 7.0 (API 24) Let's Encrypt chains
    * otherwise fail with "Trust anchor for certification path not found". Trusting it in
    * addition to the system store keeps all other CAs working unchanged.
    */
   public static synchronized SSLSocketFactory getSystemPlusLetsEncryptSocketFactory() {
      final String key = "system+isrg-root-x1";
      SSLSocketFactory factory = _sslSocketFactories.get(key);
      if (factory == null) {
         factory = createSystemPlusLetsEncryptSocketFactory();
         _sslSocketFactories.put(key, factory);
      }
      return factory;
   }

   private static SSLSocketFactory createSystemPlusLetsEncryptSocketFactory() {
      try {
         KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
         trustStore.load(null, null);

         // Seed the store with the platform's default trust anchors so that every
         // normally trusted CA keeps working.
         TrustManagerFactory systemTmf =
               TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         systemTmf.init((KeyStore) null);
         int index = 0;
         for (TrustManager tm : systemTmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
               for (X509Certificate issuer : ((X509TrustManager) tm).getAcceptedIssuers()) {
                  trustStore.setCertificateEntry("system-" + (index++), issuer);
               }
            }
         }

         // Add the bundled ISRG Root X1 (a no-op on devices that already have it).
         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         X509Certificate isrgRootX1 = (X509Certificate) cf.generateCertificate(
               new ByteArrayInputStream(ISRG_ROOT_X1_PEM.getBytes(StandardCharsets.US_ASCII)));
         trustStore.setCertificateEntry("isrg-root-x1", isrgRootX1);

         TrustManagerFactory tmf =
               TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         tmf.init(trustStore);

         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, tmf.getTrustManagers(), null);
         return sc.getSocketFactory();
      } catch (GeneralSecurityException | IOException e) {
         throw new RuntimeException(e);
      }
   }

   static {
      HOST_NAME_VERIFIER_ACCEPT_ALL = new HostnameVerifier() {
         // Used for disabling host name verification. This is safe because we
         // trust the MWAPI server certificate explicitly
         @SuppressWarnings("BadHostnameVerifier")
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
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
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
