package com.mycelium.paymentrequest;

import com.google.common.io.ByteStreams;
import com.mrd.bitlib.model.NetworkParameters;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.bitcoinj.crypto.X509Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;

public class PaymentRequestInformationTest extends TestCase {

   public void testFromRawPaymentRequestSig() throws Exception {
      byte[] rawPaymentRequest = getRawPaymentRequest("/validSig.bitcoinpaymentrequest");
      PaymentRequestInformation paymentRequestInformation = PaymentRequestInformation.fromRawPaymentRequest(rawPaymentRequest, getKeyStore(), NetworkParameters.testNetwork);
      Assert.assertTrue(paymentRequestInformation.hasValidSignature());
      Assert.assertEquals(100000000L, paymentRequestInformation.getOutputs().getTotalAmount());
      Assert.assertEquals("AddTrust AB, SE", paymentRequestInformation.getPkiVerificationData().rootAuthorityName);

   }

   public void testFromRawPaymentRequestNoSig() throws Exception {
      byte[] rawPaymentRequest = getRawPaymentRequest("/noSig.bitcoinpaymentrequest");
      PaymentRequestInformation paymentRequestInformation = PaymentRequestInformation.fromRawPaymentRequest(rawPaymentRequest, getKeyStore(), NetworkParameters.testNetwork);
      Assert.assertTrue(!paymentRequestInformation.hasValidSignature());
      Assert.assertEquals(10000, paymentRequestInformation.getOutputs().getTotalAmount());
      Assert.assertEquals(null, paymentRequestInformation.getPkiVerificationData());
   }

   private byte[] getRawPaymentRequest(String filename) throws IOException {
      File file = new File(this.getClass().getResource(filename).getFile());
      FileInputStream fileInputStream = new FileInputStream(file);
      return ByteStreams.toByteArray(fileInputStream);
   }

   public KeyStore getKeyStore() throws FileNotFoundException, KeyStoreException {

      String keystoreType;
      keystoreType = "JKS";
      File file = new File(this.getClass().getResource("/cacerts").getFile());


      return X509Utils.loadKeyStore(keystoreType, "changeit", new FileInputStream(file));
   }

}