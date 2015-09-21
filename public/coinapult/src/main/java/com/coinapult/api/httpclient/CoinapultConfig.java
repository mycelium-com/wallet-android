package com.coinapult.api.httpclient;

import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.openssl.PEMException;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.security.PublicKey;

/**
 * Created by Andreas on 30.03.2015.
 */
public abstract class CoinapultConfig {

   public abstract PublicKey getPubKey();

   public static PublicKey importPublicFromPEM(String pub) {
      try {
         Reader reader = new CharArrayReader(pub.toCharArray());
         PEMParser parser = new PEMParser(reader);
         JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
         Object obj = parser.readObject();
         parser.close();

         return converter.getPublicKey((SubjectPublicKeyInfo) obj);
      } catch (PEMException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public abstract String getBaseUrl();
}
