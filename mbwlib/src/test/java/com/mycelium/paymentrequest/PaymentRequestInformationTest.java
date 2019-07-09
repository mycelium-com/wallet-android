package com.mycelium.paymentrequest;

import com.google.common.io.ByteStreams;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.X509Utils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PaymentRequestInformationTest {
   // the test requests use certificates that expired on 2019-06-11. Mocking getTimeMillis is impossible(?)
   private static final Date DATE = (new GregorianCalendar(2019, Calendar.MARCH, 3)).getTime();
   @Test
   public void testFromRawPaymentRequestSig() throws Exception {
      byte[] rawPaymentRequest = getRawPaymentRequest("/validSig.bitcoinpaymentrequest");
      PaymentRequestInformation paymentRequestInformation = PaymentRequestInformation.fromRawPaymentRequest(rawPaymentRequest, getKeyStore(), NetworkParameters.testNetwork, DATE);
      assertTrue(paymentRequestInformation.hasValidSignature());
      assertEquals(100000000L, paymentRequestInformation.getOutputs().getTotalAmount());
      assertEquals("AddTrust AB, SE", paymentRequestInformation.getPkiVerificationData().rootAuthorityName);
   }

   @Test
   public void testFromRawPaymentRequestNoSig() throws Exception {
      byte[] rawPaymentRequest = getRawPaymentRequest("/noSig.bitcoinpaymentrequest");
      PaymentRequestInformation paymentRequestInformation = PaymentRequestInformation.fromRawPaymentRequest(rawPaymentRequest, getKeyStore(), NetworkParameters.testNetwork, DATE);
      assertTrue("", !paymentRequestInformation.hasValidSignature());
      assertEquals(10000, paymentRequestInformation.getOutputs().getTotalAmount());
      assertNull(paymentRequestInformation.getPkiVerificationData());
   }

   private byte[] getRawPaymentRequest(String filename) throws IOException {
      File file = new File(this.getClass().getResource(filename).getFile());
      FileInputStream fileInputStream = new FileInputStream(file);
      return ByteStreams.toByteArray(fileInputStream);
   }

   private KeyStore getKeyStore() throws FileNotFoundException, KeyStoreException {
      String keystoreType;
      keystoreType = "JKS";
      File file = new File(this.getClass().getResource("/cacerts").getFile());
      return X509Utils.loadKeyStore(keystoreType, "changeit", new FileInputStream(file));
   }
}