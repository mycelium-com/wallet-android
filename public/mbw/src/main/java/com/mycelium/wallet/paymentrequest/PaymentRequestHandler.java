package com.mycelium.wallet.paymentrequest;

import android.os.AsyncTask;
import android.os.Build;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.*;
import com.mycelium.wallet.BitcoinUri;
import com.squareup.okhttp.*;
import com.squareup.otto.Bus;
import com.squareup.wire.Wire;
import okio.ByteString;
import org.bitcoin.protocols.payments.*;
import org.bitcoinj.crypto.X509Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.ArrayList;

public class PaymentRequestHandler implements Serializable {

   public static final String PKI_X509_SHA256 = "x509+sha256";
   public static final String PKI_X509_SHA1 = "x509+sha1";
   public static final String PKI_NONE = "none";

   private static final int MAX_MESSAGE_SIZE = 50000;
   public static final String MAIN_NET_MONIKER = "main";
   public static final String MIME_PAYMENTREQUEST = "application/bitcoin-paymentrequest";
   public static final String MIME_ACK = "application/bitcoin-paymentack";

   private final Bus eventBus;
   private final NetworkParameters networkParameters;
   private PaymentRequestInformation paymentRequestInformation;
   private String merchantMemo;

   public PaymentRequestHandler(Bus eventBus, NetworkParameters networkParameters) {
      this.eventBus = eventBus;
      this.networkParameters = networkParameters;
   }

   public void fetchPaymentRequest(final BitcoinUri uri) {
      if (hasValidPaymentRequest()) {
         // dont refresh from the server, if we already have fetched it
         eventBus.post(paymentRequestInformation);
      } else {
         new AsyncTask<Void, Void, AsyncResultRequest>() {
            @Override
            protected AsyncResultRequest doInBackground(Void... params) {
               try {
                  PaymentRequestInformation paymentRequestInformation = fromBitcoinUri(uri);
                  return new AsyncResultRequest(paymentRequestInformation);
               } catch (PaymentRequestException ex) {
                  return new AsyncResultRequest(ex);
               }
            }

            @Override
            protected void onPostExecute(AsyncResultRequest result) {
               super.onPostExecute(result);
               if (result.exception != null) {
                  eventBus.post(result.exception);
               } else {
                  paymentRequestInformation = result.requestInformation;
                  eventBus.post(paymentRequestInformation);
               }
            }
         }.execute();
      }
   }

   // parse it from already received data and call events just like if we got it from an http call
   public void parseRawPaymentRequest(final byte[] rawPr) {
      if (hasValidPaymentRequest()) {
         // dont refresh from the server, if we already have fetched it
         eventBus.post(paymentRequestInformation);
      } else {
         try {
            paymentRequestInformation = fromRawPaymentRequest(rawPr);
            eventBus.post(paymentRequestInformation);
         } catch (PaymentRequestException ex) {
            eventBus.post(ex);
         }
      }
   }

   public PaymentRequestInformation getPaymentRequestInformation() {
      return paymentRequestInformation;
   }

   public boolean hasValidPaymentRequest() {
      return paymentRequestInformation != null;
   }

   class AsyncResultRequest {
      public final PaymentRequestException exception;
      public final PaymentRequestInformation requestInformation;

      public AsyncResultRequest(PaymentRequestException exception) {
         this.exception = exception;
         requestInformation = null;
      }

      public AsyncResultRequest(PaymentRequestInformation requestInformation) {
         this.requestInformation = requestInformation;
         exception = null;
      }
   }


   public PaymentRequestInformation fromBitcoinUri(BitcoinUri bitcoinUri) {
      Preconditions.checkNotNull(bitcoinUri.callbackURL);
      Preconditions.checkArgument(!Strings.isNullOrEmpty(bitcoinUri.callbackURL));

      // try to get the payment request from the server
      PaymentRequestInformation paymentRequestInformation = fromCallback(bitcoinUri.callbackURL);

      // if the BIP21-URI has an amount specified, check it if it matches the payment-request amount
      boolean hasBip21Amount = bitcoinUri.amount != null && bitcoinUri.amount > 0;
      boolean hasBip70Amount = paymentRequestInformation.hasAmount();
      if (hasBip21Amount && hasBip70Amount) {
         if (bitcoinUri.amount != paymentRequestInformation.getOutputs().getTotalAmount()) {
            throw new PaymentRequestException("Uri amount does not match payment request amount");
         }
      }


      return paymentRequestInformation;
   }

   private PaymentRequestInformation fromCallback(String callbackURL) {
      URL url;
      url = checkUrl(callbackURL);

      Request request = new Request.Builder()
            .url(url)
            .addHeader("Accept", MIME_PAYMENTREQUEST)
            .build();

      try {
         final OkHttpClient httpClient;
         httpClient = new OkHttpClient();  // todo: TOR?

         Response response = httpClient.newCall(request).execute();

         if (response.isSuccessful()) {
            if (!response.body().contentType().toString().equals(MIME_PAYMENTREQUEST)) {
               throw new PaymentRequestException("server responded with wrong mime-type");
            }
            byte[] data = response.body().bytes();
            return fromRawPaymentRequest(data);
         } else {
            throw new PaymentRequestException("could not fetch the payment request from " + url.toString());
         }

      } catch (IOException e) {
         throw new PaymentRequestException("server did not respond", e);
      }
   }

   private URL checkUrl(String urlString) {
      URL url;
      try {
         url = new URL(urlString);
         if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")){
            throw new PaymentRequestException("invalid protocol");
         }
      } catch (MalformedURLException e) {
         throw new PaymentRequestException("invalid url");
      }
      return url;
   }

   private PaymentRequestInformation fromRawPaymentRequest(byte[] rawPaymentRequest) {

      if (rawPaymentRequest.length > MAX_MESSAGE_SIZE) {
         throw new PaymentRequestException("payment request too large");
      }

      try {
         Wire wire = new Wire();

         PaymentRequest paymentRequest = wire.parseFrom(rawPaymentRequest, PaymentRequest.class);
         if (paymentRequest.payment_details_version != 1) {
            throw new PaymentRequestException("unsupported payment details version " + paymentRequest.payment_details_version);
         }

         PaymentDetails paymentDetails = wire.parseFrom(paymentRequest.serialized_payment_details.toByteArray(), PaymentDetails.class);

         // check if its for the correct bitcoin network (testnet/prodnet)
         if (MAIN_NET_MONIKER.equals(paymentDetails.network) != networkParameters.isProdnet()) {
            throw new PaymentRequestException("wrong network: " + paymentDetails.network);
         }

         X509Certificates certificates;
         if (!PKI_NONE.equals(paymentRequest.pki_type)) {
            if (!(paymentRequest.pki_type.equals(PKI_X509_SHA256) || paymentRequest.pki_type.equals(PKI_X509_SHA1))) {
               throw new PaymentRequestException("unsupported pki type " + paymentRequest.pki_type);
            }

            if (paymentRequest.pki_data == null || paymentRequest.pki_data.size() == 0) {
               throw new PaymentRequestException("no pki data available");
            }

            if (paymentRequest.signature == null || paymentRequest.signature.size() == 0) {
               throw new PaymentRequestException("no signature available");
            }

            certificates = wire.parseFrom(paymentRequest.pki_data.toByteArray(), X509Certificates.class);
            PkiVerificationData pkiVerificationData = verifySignature(paymentRequest, certificates);
            return new PaymentRequestInformation(paymentRequest, paymentDetails, pkiVerificationData);


         } else {
            return new PaymentRequestInformation(paymentRequest, paymentDetails, null);
         }


      } catch (IOException e) {
         throw new PaymentRequestException("invalid formatted payment request", e);
      }
   }

   private static KeyStore getAndroidKeyStore() {
      KeyStore trustStore;

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
         try {
            trustStore = KeyStore.getInstance("AndroidCAStore");
            trustStore.load(null, null);
         } catch (KeyStoreException e) {
            throw new PaymentRequestException("unable to access keystore", e);
         } catch (CertificateException e) {
            throw new PaymentRequestException("unable to access keystore", e);
         } catch (NoSuchAlgorithmException e) {
            throw new PaymentRequestException("unable to access keystore", e);
         } catch (IOException e) {
            throw new PaymentRequestException("unable to access keystore", e);
         }
      } else {
         // we have minSdk == ICS
         throw new PaymentRequestException("unsupported keystore");
      }

      return trustStore;
   }

   private static String getPkiSignatureAlgorithm(PaymentRequest paymentRequest) {
      if (PKI_X509_SHA256.equals(paymentRequest.pki_type)) {
         return "SHA256withRSA";
      } else if (PKI_X509_SHA1.equals(paymentRequest.pki_type)) {
         return "SHA1withRSA";
      } else {
         throw new PaymentRequestException("unsupported signature algorithm");
      }
   }

   private static PkiVerificationData verifySignature(PaymentRequest paymentRequest, X509Certificates certificates) {
      if (certificates == null) {
         throw new PaymentRequestException("no certificates supplied");
      }

      try {
         KeyStore keyStore = getAndroidKeyStore();

         CertificateFactory certFact = CertificateFactory.getInstance("X.509");

         // parse each certificate from the chain ...
         ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
         for (ByteString cert : certificates.certificate) {
            ByteArrayInputStream inStream = new ByteArrayInputStream(cert.toByteArray());
            certs.add((X509Certificate) certFact.generateCertificate(inStream));
         }

         // ... and generate the certification path from it.
         CertPath certPath = certFact.generateCertPath(certs);

         // Retrieves the most-trusted CAs from keystore.
         PKIXParameters params = new PKIXParameters(keyStore);
         // Revocation not supported in the current version.
         params.setRevocationEnabled(false);

         // Now verify the certificate chain is correct and trusted. This let's us get an identity linked pubkey.
         CertPathValidator validator = CertPathValidator.getInstance("PKIX");
         PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, params);
         PublicKey publicKey = result.getPublicKey();

         // OK, we got an identity, now check it was used to sign this message.
         Signature signature = Signature.getInstance(getPkiSignatureAlgorithm(paymentRequest));
         // Note that we don't use signature.initVerify(certs.get(0)) here despite it being the most obvious
         // way to set it up, because we don't care about the constraints specified on the certificates: any
         // cert that links a key to a domain name or other identity will do for us.
         signature.initVerify(publicKey);

         // duplicate the payment-request but with an empty signature
         // then check the again serialized format of it
         PaymentRequest checkPaymentRequest = new PaymentRequest.Builder(paymentRequest)
               .signature(ByteString.EMPTY)
               .build();

         // serialize the payment request (now with an empty signature field) and check if the signature verifies
         signature.update(checkPaymentRequest.toByteArray());

         boolean isValid = signature.verify(paymentRequest.signature.toByteArray());

         if (!isValid) {
            throw new PaymentRequestException("signature does not match");
         }


         // Signature verifies, get the names from the identity we just verified for presentation to the user.
         final X509Certificate cert = certs.get(0);
         //return new PkiVerificationData(displayName, publicKey, result.getTrustAnchor());
         String displayName = X509Utils.getDisplayNameFromCertificate(cert, true);
         return new PkiVerificationData(displayName, publicKey, result.getTrustAnchor());


      } catch (CertificateException e) {
         throw new PaymentRequestException("invalid certificate", e);
      } catch (InvalidKeyException e) {
         throw new PaymentRequestException("keystore not ready", e);
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (InvalidAlgorithmParameterException e) {
         throw new PaymentRequestException("invalid certificate", e);
      } catch (KeyStoreException e) {
         throw new RuntimeException(e);
      } catch (CertPathValidatorException e) {
         throw new PaymentRequestException("invalid certificate", e);
      } catch (SignatureException e) {
         throw new PaymentRequestException("invalid certificate", e);
      }

   }

   public void setMerchantMemo(String memo) {
      merchantMemo = memo;
   }

   public boolean sendResponse(final Transaction signedTransaction, final Address refundAddress) {
      if (hasValidPaymentRequest() && !Strings.isNullOrEmpty(paymentRequestInformation.getPaymentDetails().payment_url)) {
         new AsyncTask<Void, Void, AsyncResultAck>() {
            @Override
            protected AsyncResultAck doInBackground(Void... params) {
               Payment payment = getPaymentRequestInformation().buildPaymentResponse(refundAddress, merchantMemo, signedTransaction);
               try {
                  PaymentACK paymentAck = sendPaymentResponse(payment);
                  return new AsyncResultAck(paymentAck);
               } catch (PaymentRequestException ex) {
                  return new AsyncResultAck(ex);
               }
            }

            @Override
            protected void onPostExecute(AsyncResultAck paymentACK) {
               if (paymentACK.exception != null) {
                  eventBus.post(paymentACK.exception);
               } else {
                  eventBus.post(paymentACK.paymentAck);
               }
            }
         }.execute();
         return true;
      } else {
         return false;
      }
   }

   class AsyncResultAck {
      public final PaymentACK paymentAck;
      public final PaymentRequestException exception;

      public AsyncResultAck(PaymentRequestException exception) {
         this.exception = exception;
         paymentAck = null;
      }

      public AsyncResultAck(PaymentACK paymentAck) {
         this.paymentAck = paymentAck;
         exception = null;
      }
   }


   private PaymentACK sendPaymentResponse(Payment payment) {
      RequestBody requestBody = RequestBody.create(MediaType.parse("application/bitcoin-payment"), payment.toByteArray());

      URL url = checkUrl(paymentRequestInformation.getPaymentDetails().payment_url);

      Request request = new Request.Builder()
            .url(url)
            .addHeader("Accept", MIME_ACK)
            .post(requestBody)
            .build();


      try {
         final OkHttpClient httpClient;
         httpClient = new OkHttpClient();  // todo: TOR?
         Response response = httpClient.newCall(request).execute();

         Wire wire = new Wire();

         PaymentACK paymentAck;
         if (response.isSuccessful()) {

            if (!response.body().contentType().toString().equals(MIME_ACK)) {
               throw new PaymentRequestException("server responded with wrong ack mime-type");
            }

            byte[] data = response.body().bytes();
            if (data.length > MAX_MESSAGE_SIZE) {
               throw new PaymentRequestException("ack-message too large");
            }
            paymentAck = wire.parseFrom(data, PaymentACK.class);
            return paymentAck;
         } else {
            throw new PaymentRequestException("could not fetch the payment request from " + paymentRequestInformation.getPaymentDetails().payment_url);
         }


      } catch (IOException e) {
         throw new PaymentRequestException("server did not respond with an payment ack", e);
      }

   }

}

