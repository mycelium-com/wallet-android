package com.coinapult.api.httpclient;

import org.spongycastle.asn1.*;
import org.spongycastle.openssl.PEMKeyPair;
import org.spongycastle.openssl.PEMParser;
import org.spongycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;

import java.io.*;
import java.math.BigInteger;
import java.security.*;

public class ECC_SC implements EccUtil {
   private static final String ECDSA = "SHA256withECDSA";

   public boolean verifySign(String signature, String origdata, PublicKey pub) {
      try {
         Signature dsa = Signature.getInstance(ECDSA);
         dsa.initVerify(pub);
         dsa.update(origdata.getBytes());

		/* Construct ASN1 sequence from the signature received. */
         BigInteger r = new BigInteger(signature.substring(0, 64), 16);
         BigInteger s = new BigInteger(signature.substring(64,
               signature.length()), 16);
         ASN1EncodableVector vec = new ASN1EncodableVector();
         vec.add(new ASN1Integer(r));
         vec.add(new ASN1Integer(s));
         ASN1Sequence seq = new DERSequence(vec);

         byte[] sign = seq.getEncoded();
         return dsa.verify(sign);
      } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public String exportToPEM(Key eccPub) {
      try {
         Writer writer = new CharArrayWriter();
         JcaPEMWriter pemwriter = new JcaPEMWriter(writer);
         pemwriter.writeObject(eccPub);
         pemwriter.flush();
         String result = writer.toString();
         pemwriter.close();
         return result.trim();
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public KeyPair importFromPEM(String priv) {
      try {
         Reader reader = new CharArrayReader(priv.toCharArray());
         PEMParser parser = new PEMParser(reader);
         JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
         Object obj = parser.readObject();
         parser.close();

         return converter.getKeyPair((PEMKeyPair) obj);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public PublicKey getCoinapultPubKey3() {
      BigInteger x= new BigInteger("40989861273222514956482323324142182379712520051136673709789400227216002921550");
      BigInteger y= new BigInteger("75636774962004883403879667389924175484183873092966382079409006435518206433630");
      return AndroidKeyConverter.makePubKey(x, y);
   }

   @Override
   public String generateSign(String signdata, PrivateKey eccPriv) {
      try {
         Signature dsa = Signature.getInstance(ECDSA);
         dsa.initSign(eccPriv);
         dsa.update(signdata.getBytes());
         byte[] sign = dsa.sign();

         ASN1InputStream decoder = new ASN1InputStream(sign);
         ASN1Sequence seq = (ASN1Sequence) decoder.readObject();
         BigInteger r = ((ASN1Integer) seq.getObjectAt(0)).getValue();
         BigInteger s = ((ASN1Integer) seq.getObjectAt(1)).getValue();
         decoder.close();

         String first = r.toString(16);
         String second = s.toString(16);
         return first +"|"+ second;
      } catch (IOException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
         throw new RuntimeException(e);
      }
   }
}
