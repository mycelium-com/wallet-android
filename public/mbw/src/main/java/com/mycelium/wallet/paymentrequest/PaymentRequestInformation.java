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

package com.mycelium.wallet.paymentrequest;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mrd.bitlib.model.*;
import okio.ByteString;
import org.bitcoin.protocols.payments.Output;
import org.bitcoin.protocols.payments.Payment;
import org.bitcoin.protocols.payments.PaymentDetails;
import org.bitcoin.protocols.payments.PaymentRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// container-class for all deserialized/parsed and checked information regarding a payment request
public class PaymentRequestInformation implements Serializable {

   private final PaymentRequest paymentRequest;
   private final PaymentDetails paymentDetails;
   private final PkiVerificationData pkiVerificationData;


   public PaymentRequestInformation(PaymentRequest paymentRequest, PaymentDetails paymentDetails, PkiVerificationData pkiVerificationData) {
      this.paymentRequest = paymentRequest;
      this.paymentDetails = paymentDetails;
      this.pkiVerificationData = pkiVerificationData;
   }

   public OutputList getOutputs() {
      OutputList ret = new OutputList();
      for (Output out : paymentDetails.outputs) {
         ret.add(out.amount, ScriptOutput.fromScriptBytes(out.script.toByteArray()));
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
      byte[] scriptBytes = new ScriptOutputStandard(refundAddress.getTypeSpecificBytes()).getScriptBytes();
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

   public PaymentDetails getPaymentDetails() {
      return paymentDetails;
   }

   public boolean hasPaymentCallbackUrl() {
      return paymentDetails != null && !Strings.isNullOrEmpty(paymentDetails.payment_url);
   }
}

