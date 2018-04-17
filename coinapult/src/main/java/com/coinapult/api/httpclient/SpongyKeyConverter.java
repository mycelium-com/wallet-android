package com.coinapult.api.httpclient;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.ec.Point;
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class SpongyKeyConverter {
   public static KeyPair convertKeyFormat(InMemoryPrivateKey inMemoryPrivateKey) {
      try {
         org.spongycastle.jce.spec.ECNamedCurveParameterSpec secp256k1 = org.spongycastle.jce.ECNamedCurveTable.getParameterSpec("secp256k1");
         org.spongycastle.jce.spec.ECPrivateKeySpec privSpec = new org.spongycastle.jce.spec.ECPrivateKeySpec(new BigInteger(1, inMemoryPrivateKey.getPrivateKeyBytes()), secp256k1);
         KeyFactory keyFactory = KeyFactory.getInstance("EC",BouncyCastleProvider.PROVIDER_NAME);
         BCECPrivateKey bcpriv = (BCECPrivateKey) keyFactory.generatePrivate(privSpec);
         Point pubPoint = inMemoryPrivateKey.getPublicKey().getQ();
         org.spongycastle.math.ec.ECPoint ecpubPoint = new org.spongycastle.math.ec.custom.sec.SecP256K1Curve().createPoint(pubPoint.getX().toBigInteger(), pubPoint.getY().toBigInteger());
         org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey publicKey = (org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey) keyFactory.generatePublic(new org.spongycastle.jce.spec.ECPublicKeySpec(ecpubPoint, secp256k1));
         return new KeyPair(publicKey, bcpriv);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchProviderException e) {
         throw new RuntimeException(e);
      }
   }
}
