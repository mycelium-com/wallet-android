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

package com.mycelium.wallet.extsig.keepkey.activity;

import android.widget.ImageView;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wallet.extsig.common.activity.ExtSigSignTransactionActivity;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;

public class KeepKeySignTransactionActivity
      extends ExtSigSignTransactionActivity
      implements MasterseedPasswordSetter {


   @Override
   protected ExternalSignatureDeviceManager getExtSigManager() {
      return MbwManager.getInstance(this).getKeepKeyManager();
   }

   @Override
   protected void setView() {
      setContentView(R.layout.sign_ext_sig_transaction_activity);
      ((ImageView)findViewById(R.id.ivConnectExtSig)).setImageResource(R.drawable.connect_keepkey);
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event) {
      super.onPassphraseRequest(event);
   }

   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event) {
      super.onScanError(event);
   }

   @Override
   @Subscribe
   public void onStatusUpdate(ExternalSignatureDeviceManager.OnStatusUpdate event) {
      super.onStatusUpdate(event);
   }

   @Subscribe
   public void onPinMatrixRequest(ExternalSignatureDeviceManager.OnPinMatrixRequest event) {
      super.onPinMatrixRequest(event);
   }

   @Subscribe
   public void onButtonRequest(ExternalSignatureDeviceManager.OnButtonRequest event) {
      super.onButtonRequest(event);
   }


   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event) {
      super.onStatusChanged(event);
   }


}
