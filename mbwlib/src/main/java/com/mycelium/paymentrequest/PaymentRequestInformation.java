/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.paymentrequest;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mrd.bitlib.model.*;
import com.squareup.wire.Wire;
import okio.ByteString;
import org.bitcoin.protocols.payments.*;
import org.bitcoinj.crypto.X509Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.*;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Date;

// container-class for all deserialized/parsed and checked information regarding a payment request
public class PaymentRequestInformation implements Serializable {
   public static final int MAX_MESSAGE_SIZE = 50000;
   private static final String MAIN_NET_MONIKER = "main";

   public static final String PKI_X509_SHA256 = "x509+sha256";
   public static final String PKI_X509_SHA1 = "x509+sha1";
   public static final String PKI_NONE = "none";


   private final PaymentRequest paymentRequest;
   private final PaymentDetails paymentDetails;
   private final PkiVerificationData pkiVerificationData;
   private final byte[] rawPaymentRequest;

   public static PaymentRequestInformation fromRawPaymentRequest(byte[] rawPaymentRequest, KeyStore keyStore, final NetworkParameters networkParameters) {

      if (rawPaymentRequest.length > MAX_MESSAGE_SIZE) {
         throw new PaymentRequestException("payment request too large");
      }

      try {
         Wire wire = new Wire();

         PaymentRequest paymentRequest = wire.parseFrom(rawPaymentRequest, PaymentRequest.class);
         if (paymentRequest == null) {
            throw new PaymentRequestException("unable to parse the payment request");
         }

         Integer version = Wire.get(paymentRequest.payment_details_version, PaymentRequest.DEFAULT_PAYMENT_DETAILS_VERSION);
         if (version != 1) {
            throw new PaymentRequestException("unsupported payment details version " + version);
         }

         PaymentDetails paymentDetails = wire.parseFrom(paymentRequest.serialized_payment_details.toByteArray(), PaymentDetails.class);
         if (paymentDetails == null) {
            throw new PaymentRequestException("unable to parse the payment details");
         }

         // check if its for the correct bitcoin network (testnet/prodnet)
         if (MAIN_NET_MONIKER.equals(Wire.get(paymentDetails.network, PaymentDetails.DEFAULT_NETWORK)) != networkParameters.isProdnet()) {
            throw new PaymentRequestException("wrong network: " + Wire.get(paymentDetails.network, PaymentDetails.DEFAULT_NETWORK));
         }

         if (Wire.get(paymentDetails.outputs, PaymentDetails.DEFAULT_OUTPUTS).size() == 0) {
            throw new PaymentRequestException("no outputs specified");
         }

         // check if we are able to parse all output scripts
         // we might need to improve this later on to provide some flexibility, but until there is are use-cases
         // prevent users from sending coins to maybe unspendable outputs
         OutputList transactionOutputs = getTransactionOutputs(paymentDetails);
         boolean containsStrangeOutput = Iterables.any(transactionOutputs, new Predicate<TransactionOutput>() {
            @Override
            public boolean apply(TransactionOutput input) {
               // search if we got a strange output or a null address as destination
               return input.script instanceof ScriptOutputStrange ||
                     input.script.getAddress(networkParameters).equals(Address.getNullAddress(networkParameters));
            }
         });

         if (containsStrangeOutput) {
            throw new PaymentRequestException("unable to parse one of the output scripts");
         }

         X509Certificates certificates;
         String pki_type = Wire.get(paymentRequest.pki_type, PaymentRequest.DEFAULT_PKI_TYPE);
         if (!PKI_NONE.equals(pki_type)) {
            if (!(pki_type.equals(PKI_X509_SHA256) || pki_type.equals(PKI_X509_SHA1))) {
               throw new PaymentRequestException("unsupported pki type " + pki_type);
            }

            if (paymentRequest.pki_data == null || paymentRequest.pki_data.size() == 0) {
               throw new PaymentRequestException("no pki data available");
            }

            if (paymentRequest.signature == null || paymentRequest.signature.size() == 0) {
               throw new PaymentRequestException("no signature available");
            }

            certificates = wire.parseFrom(paymentRequest.pki_data.toByteArray(), X509Certificates.class);
            PkiVerificationData pkiVerificationData = verifySignature(paymentRequest, certificates, keyStore);
            return new PaymentRequestInformation(paymentRequest, paymentDetails, pkiVerificationData, rawPaymentRequest);


         } else {
            return new PaymentRequestInformation(paymentRequest, paymentDetails, null, rawPaymentRequest);
         }


      } catch (IOException e) {
         throw new PaymentRequestException("invalid formatted payment request", e);
      }
   }

   private static PkiVerificationData verifySignature(PaymentRequest paymentRequest, X509Certificates certificates, KeyStore keyStore) {
      if (certificates == null) {
         throw new PaymentRequestException("no certificates supplied");
      }

      try {

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

   private static String getPkiSignatureAlgorithm(PaymentRequest paymentRequest) {
      if (PKI_X509_SHA256.equals(paymentRequest.pki_type)) {
         return "SHA256withRSA";
      } else if (PKI_X509_SHA1.equals(paymentRequest.pki_type)) {
         return "SHA1withRSA";
      } else {
         throw new PaymentRequestException("unsupported signature algorithm");
      }
   }


   public PaymentRequestInformation(PaymentRequest paymentRequest, PaymentDetails paymentDetails, PkiVerificationData pkiVerificationData, byte[] rawPaymentRequest) {
      this.paymentRequest = paymentRequest;
      this.paymentDetails = paymentDetails;
      this.pkiVerificationData = pkiVerificationData;
      this.rawPaymentRequest = rawPaymentRequest;
   }

   public OutputList getOutputs() {
      return getTransactionOutputs(this.paymentDetails);
   }

   private static OutputList getTransactionOutputs(PaymentDetails paymentDetails) {
      OutputList ret = new OutputList();
      for (Output out : paymentDetails.outputs) {
         ret.add(
               Wire.get(out.amount, Output.DEFAULT_AMOUNT),
               ScriptOutput.fromScriptBytes(Wire.get(out.script, Output.DEFAULT_SCRIPT).toByteArray()));
      }
      return ret;
   }

   // try to parse the output scripts and get associated addresses - not all output scripts may be parse-able
   public ArrayList<Address> getKnownOutputAddresses(NetworkParameters networkParameters) {
      ArrayList<Address> ret = new ArrayList<Address>();
      for (Output out : paymentDetails.outputs) {
         ScriptOutput scriptOutput = ScriptOutput.fromScriptBytes(out.script.toByteArray());
         if (!(scriptOutput instanceof ScriptOutputStrange)) {
            ret.add(scriptOutput.getAddress(networkParameters));
         }
      }
      return ret;
   }

   public boolean hasAmount() {
      return getOutputs().getTotalAmount() > 0;
   }

   public boolean hasValidSignature() {
      return pkiVerificationData != null;
   }

   public PkiVerificationData getPkiVerificationData() {
      return pkiVerificationData;
   }


   public Payment buildPaymentResponse(Address refundAddress, String memo, Transaction signedTransaction) {
      byte[] scriptBytes = new ScriptOutputP2PKH(refundAddress.getTypeSpecificBytes()).getScriptBytes();
      Output refundOutput = new Output.Builder()
            .amount(getOutputs().getTotalAmount())
            .script(ByteString.of(scriptBytes))
            .build();


      Payment payment = new Payment.Builder()
            .merchant_data(paymentDetails.merchant_data)
            .refund_to(Lists.newArrayList(refundOutput))
            .memo(memo)
            .transactions(Lists.newArrayList(ByteString.of(signedTransaction.toBytes())))
            .build();

      return payment;

   }

   public boolean isExpired() {
      if (paymentDetails == null || paymentDetails.expires == null || paymentDetails.expires == 0) {
         // no expire-date set
         return false;
      }

      Date expireDate = new Date(paymentDetails.expires * 1000L);
      return expireDate.getTime() <= new Date().getTime();
   }

   public PaymentRequest getPaymentRequest() {
      return paymentRequest;
   }

   public byte[] getRawPaymentRequest() {
      return rawPaymentRequest;
   }

   public PaymentDetails getPaymentDetails() {
      return paymentDetails;
   }

   public boolean hasPaymentCallbackUrl() {
      return paymentDetails != null && !Strings.isNullOrEmpty(paymentDetails.payment_url);
   }
}

