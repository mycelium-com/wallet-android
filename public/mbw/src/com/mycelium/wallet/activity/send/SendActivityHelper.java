/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mycelium.wallet.activity.send;

import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.SpendableOutputs;

public class SendActivityHelper {

   public enum WalletSource {
      Specified, SelectPrivateKey, InstantWallet
   };

   public static class SendContext implements Serializable {
      private static final long serialVersionUID = 1L;

      private WalletSource walletMode;
      public Wallet wallet;
      public Address receivingAddress;
      public Long amountToSend;
      public SpendableOutputs spendableOutputs;

      private SendContext(WalletSource walletMode) {
         this.walletMode = walletMode;
      }
   }

   public static void startSendActivity(Activity current, Address receivingAddress, Long amountToSend,
         WalletSource walletMode, Wallet wallet) {
      SendContext context = new SendContext(walletMode);
      if (context.walletMode == WalletSource.Specified) {
         context.wallet = wallet;
      }
      context.receivingAddress = receivingAddress;
      context.amountToSend = amountToSend;
      startNextActivity(current, context);
   }

   public static void startNextActivity(Activity current, Address receivingAddress, Long amountToSend) {
      SendContext context = getSendContext(current);
      context.receivingAddress = receivingAddress;
      context.amountToSend = amountToSend;
      startNextActivity(current, context);
      current.finish();
   }

   public static void startNextActivity(Activity current, Address receivingAddress) {
      SendContext context = getSendContext(current);
      context.receivingAddress = receivingAddress;
      startNextActivity(current, context);
      current.finish();
   }

   public static void startNextActivity(Activity current, Wallet wallet) {
      SendContext context = getSendContext(current);
      context.wallet = wallet;
      startNextActivity(current, context);
      current.finish();

   }

   public static void startNextActivity(Activity current, Long amountToSend) {
      SendContext context = getSendContext(current);
      context.amountToSend = amountToSend;
      startNextActivity(current, context);
      current.finish();
   }

   public static void startNextActivity(Activity current, SpendableOutputs spendableOutputs) {
      SendContext context = getSendContext(current);
      context.spendableOutputs = spendableOutputs;
      startNextActivity(current, context);
      current.finish();
   }

   private static void startNextActivity(Activity current, SendContext context) {
      Intent intent;
      if (context.wallet == null) {
         if (context.walletMode == WalletSource.SelectPrivateKey) {
            intent = new Intent(current, GetSpendingRecordActivity.class);
         } else {
            intent = new Intent(current, InstantWalletActivity.class);
         }
      } else if (context.receivingAddress == null) {
         intent = new Intent(current, GetAddressActivity.class);
      } else if (context.spendableOutputs == null) {
         intent = new Intent(current, GetUnspentOutputsActivity.class);
      } else if (context.amountToSend == null || context.amountToSend == 0) {
         intent = new Intent(current, GetSendingAmountActivity.class);
      } else {
         // We have it all
         intent = new Intent(current, SendSummaryActivity.class);
      }
      intent.putExtra("sendContext", context);
      current.startActivity(intent);
   }

   public static SendContext getSendContext(Activity current) {
      SendContext context = (SendContext) current.getIntent().getSerializableExtra("sendContext");
      return context;
   }
}
