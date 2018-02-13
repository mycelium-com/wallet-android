package com.coinapult.api.httpclient;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.WapiLogger;
import org.junit.Ignore;
import org.junit.Test;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

public class ExampleTest {

// doctor vote original ramp spoil craft bid half amazing index dinosaur smile


   /*@Test
   public void bcscPubkey() {
      Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
      Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());

      org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey coinapultPubkeyBC =
            (org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey) new ECC_SC().importPublicFromPEM(ECC_SC.COINAPULT_PUBLIC_KEY);

//      org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey coinapultPubkeySC = new org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey()
      BigInteger x = coinapultPubkeyBC.getQ().getXCoord().toBigInteger();
      BigInteger y = coinapultPubkeyBC.getQ().getYCoord().toBigInteger();
      ECC_SC sc = new ECC_SC();
      SecP256K1Curve curve = new SecP256K1Curve();
      ECParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
      ECPublicKeySpec pubSpec = new ECPublicKeySpec(curve.createPoint(x, y), spec);
      BCECPublicKey ECPUKEY = new BCECPublicKey("EC", pubSpec, org.spongycastle.jce.provider.BouncyCastleProvider.CONFIGURATION);
//      String SC_PUBKEY = sc.exportToPEM(coinapultPubkeySC);
      System.out.println(ECC_SC.COINAPULT_PUBLIC_KEY);
//    x=  40989861273222514956482323324142182379712520051136673709789400227216002921550
//    y=  75636774962004883403879667389924175484183873092966382079409006435518206433630
      System.out.println("\n\n");
      System.out.println(sc.exportToPEM(ECPUKEY));

   }*/

   @Test
   @Ignore
   public void runSimpleCreate() throws IOException, CoinapultError.CoinapultExceptionECC, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
      InMemoryPrivateKey randomKey = new InMemoryPrivateKey(new TestRandom());
      CoinapultClient coinapultClient = new CoinapultClient(AndroidKeyConverter.convertKeyFormat(randomKey), new ECC_SC(), new CoinapultPlaygroundConfig(), WapiLogger.NULL_LOGGER);
      coinapultClient.createAccount();
      coinapultClient.activateAccount(true);
   }

   @Test
   @Ignore
   public void runSimpleUSD() throws IOException, CoinapultError.CoinapultExceptionECC, NoSuchAlgorithmException, InvalidKeyException, SignatureException, NoSuchProviderException, CoinapultClient.CoinapultBackendException {
      InMemoryPrivateKey randomKey = new InMemoryPrivateKey(new TestRandom());
      CoinapultClient coinapultClient = new CoinapultClient(AndroidKeyConverter.convertKeyFormat(randomKey), new ECC_SC(), new CoinapultPlaygroundConfig(), WapiLogger.NULL_LOGGER);
      coinapultClient.accountExists();
      coinapultClient.createAccount();
      coinapultClient.activateAccount(true);
      Address.Json bitcoinAddress = coinapultClient.getBitcoinAddress();
      coinapultClient.config(bitcoinAddress.address, "USD");
   }

   @Test
   @Ignore
   public void runTest() {
      Security.insertProviderAt(new BouncyCastleProvider(), 1);

		/*
       * Assuming there is no previous key pair, one has to be created now.
		 */
      ECC_SC ecc_bc = new ECC_SC();
      try {
         KeyPair keypair;
         KeyPairGenerator keygen = KeyPairGenerator.getInstance("EC",
               org.spongycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME);
         ECGenParameterSpec eccspec = new ECGenParameterSpec("secp256k1");
         keygen.initialize(eccspec);

         keypair = keygen.generateKeyPair();
         keypair.getPrivate();

         ecc_bc.exportToPEM(keypair.getPrivate());

      } catch (Throwable err) {
         err.printStackTrace();
         System.exit(1);
         return;
      }

      try {
         InMemoryPrivateKey priv = new InMemoryPrivateKey(new TestRandom());
         System.out.println("creating account bound to address " + priv.getPublicKey().toAddress(NetworkParameters.productionNetwork));

         CoinapultClient cli = new CoinapultClient(SpongyKeyConverter.convertKeyFormat(priv), new ECC_SC(), new CoinapultProdConfig(), WapiLogger.NULL_LOGGER);
         /* Create a new account. */
         AccountNew.JsonNew res = cli.createAccount();
         cli.activateAccount(true);
         System.out.println(res.toPrettyString());
         /* Read and agree to the terms before activating the new account. */
         AccountNew.Json conf = cli.activateAccount(true);
         System.out.println(conf.toPrettyString());
         /* Start a lock transaction. */
         Transaction.Json lock = cli.lock(0.5, 0, "USD", null);
         System.out.println(lock.toPrettyString());

         Address.Json bitcoinAddress = cli.getBitcoinAddress();
         System.out.println(bitcoinAddress);
      } catch (Throwable err) {
         err.printStackTrace();
      }
   }

   private class TestRandom implements RandomSource {
      private SecureRandom _secureRandom;

      TestRandom(){
         _secureRandom = new SecureRandom();
      }

      @Override
      public void nextBytes(byte[] bytes) {
         _secureRandom.nextBytes(bytes);
      }
   }
}
