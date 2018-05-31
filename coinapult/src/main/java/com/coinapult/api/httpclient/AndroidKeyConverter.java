package com.coinapult.api.httpclient;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.ec.Point;

import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.custom.sec.SecP256K1Curve;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

public class AndroidKeyConverter {
   public static PublicKey makePubKey(BigInteger x, BigInteger y)  {
      try {
         KeyFactory keyFactory = KeyFactory.getInstance("EC");
         ECParameterSpec secp256k1 = getSecp256k1Spec();

         org.spongycastle.math.ec.ECPoint ecpubPoint_SC = new SecP256K1Curve().createPoint(x, y);
//         ECPoint ecpubPoint = new SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
         ECPoint ecpubPoint = convertECPoint(ecpubPoint_SC);
         ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(ecpubPoint, secp256k1));
         return publicKey;
      } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      }
   }

   public static KeyPair convertKeyFormat(InMemoryPrivateKey inMemoryPrivateKey) {
      try {
         ECNamedCurveParameterSpec secp256k1 = org.spongycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
         org.spongycastle.jce.spec.ECPrivateKeySpec privSpec = new org.spongycastle.jce.spec.ECPrivateKeySpec(new BigInteger(1, inMemoryPrivateKey.getPrivateKeyBytes()), secp256k1);
         KeyFactory keyFactory = KeyFactory.getInstance("EC","SC");
//         com.android.org.bouncycastle.jce.provider.BouncycastleProvider ??
         PrivateKey bcpriv = keyFactory.generatePrivate(privSpec);
         Point pubPoint = inMemoryPrivateKey.getPublicKey().getQ();
         org.spongycastle.math.ec.ECPoint ecpubPoint = new org.spongycastle.math.ec.custom.sec.SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
         PublicKey publicKey = keyFactory.generatePublic(new org.spongycastle.jce.spec.ECPublicKeySpec(ecpubPoint, secp256k1));
         return new KeyPair(publicKey, bcpriv);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (NoSuchProviderException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    *
    * @param inMemoryPrivateKey
    * @return an openssl style key
    */
   public static KeyPair convertKeyFormatAndroid(InMemoryPrivateKey inMemoryPrivateKey)  {

      try {
         ECParameterSpec secp256k1 = getSecp256k1Spec();

         ECPrivateKeySpec privSpec = new ECPrivateKeySpec(new BigInteger(1, inMemoryPrivateKey.getPrivateKeyBytes()), secp256k1);
         KeyFactory keyFactory = KeyFactory.getInstance("EC");
         ECPrivateKey bcpriv = (ECPrivateKey) keyFactory.generatePrivate(privSpec);
         Point pubPoint = inMemoryPrivateKey.getPublicKey().getQ();
         org.spongycastle.math.ec.ECPoint ecpubPoint_SC = new SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
//         ECPoint ecpubPoint = new SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
         ECPoint ecpubPoint = convertECPoint(ecpubPoint_SC);
         ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(ecpubPoint,secp256k1));
         return new KeyPair(publicKey, bcpriv);
      } catch (InvalidKeySpecException e) {
         throw new RuntimeException(e);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
//      } catch (NoSuchProviderException e) {
//         throw new RuntimeException(e);
      }
   }

   public static KeyPair convertKeyFormatSpongy(InMemoryPrivateKey inMemoryPrivateKey)  {

      try {
         ECParameterSpec secp256k1 = getSecp256k1Spec();

         ECPrivateKeySpec privSpec = new ECPrivateKeySpec(new BigInteger(1, inMemoryPrivateKey.getPrivateKeyBytes()), secp256k1);
         KeyFactory keyFactory = KeyFactory.getInstance("EC");
         ECPrivateKey bcpriv = (ECPrivateKey) keyFactory.generatePrivate(privSpec);
         Point pubPoint = inMemoryPrivateKey.getPublicKey().getQ();
         org.spongycastle.math.ec.ECPoint ecpubPoint_SC = new SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
//         ECPoint ecpubPoint = new SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
         ECPoint ecpubPoint = convertECPoint(ecpubPoint_SC);
         ECPublicKey publicKey = (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(ecpubPoint,secp256k1));
         return new KeyPair(publicKey, bcpriv);
      } catch (InvalidKeySpecException e) {
         throw new RuntimeException(e);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
//      } catch (NoSuchProviderException e) {
//         throw new RuntimeException(e);
      }
   }

   private static ECParameterSpec getSecp256k1Spec() {
      org.spongycastle.jce.spec.ECParameterSpec secp256k1_SC = ECNamedCurveTable.getParameterSpec("secp256k1");
      org.spongycastle.math.ec.ECPoint g = secp256k1_SC.getG();
      ECPoint g1 = convertECPoint(g);
      EllipticCurve curve = EC5Util.convertCurve(secp256k1_SC.getCurve(), null);
      BigInteger n = secp256k1_SC.getN();
      int h = secp256k1_SC.getH().intValue();
      return new ECParameterSpec(curve, g1, n, h);
   }

   private static ECPoint convertECPoint(org.spongycastle.math.ec.ECPoint g) {
      return new ECPoint(g.getXCoord().toBigInteger(), g.getYCoord().toBigInteger());
   }
}
