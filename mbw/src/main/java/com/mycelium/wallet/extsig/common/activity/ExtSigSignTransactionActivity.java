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

package com.mycelium.wallet.extsig.common.activity;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.MasterseedPasswordDialog;
import com.mycelium.wallet.activity.send.SignTransactionActivity;
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

public abstract class ExtSigSignTransactionActivity
      extends SignTransactionActivity
      implements MasterseedPasswordSetter {

   private static final String PASSPHRASE_FRAGMENT_TAG = "pass";

   abstract protected ExternalSignatureDeviceManager getExtSigManager();

   private boolean showTx;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
   }

   @Override
   protected void onResume() {
      super.onResume();
      MbwManager.getInstance(this).getEventBus().register(this);
      updateUi();
   }

   @Override
   protected void onPause() {
      MbwManager.getInstance(this).getEventBus().unregister(this);
      super.onPause();
   }


   @Override
   public void setPassphrase(String passphrase){
      getExtSigManager().setPassphrase(passphrase);

      if (passphrase == null){
         // user choose cancel -> leave this activity
         finish();
      } else {
         // close the dialog fragment
         Fragment fragPassphrase = getFragmentManager().findFragmentByTag(PASSPHRASE_FRAGMENT_TAG);
         if (fragPassphrase != null) {
            getFragmentManager().beginTransaction().remove(fragPassphrase).commit();
         }
      }
   }

   private void updateUi(){
      if (getExtSigManager().getCurrentState() != ExternalSignatureDeviceManager.Status.unableToScan){
         findViewById(R.id.ivConnectExtSig).setVisibility(View.GONE);
         findViewById(R.id.tvPluginDevice).setVisibility(View.GONE);
      } else {
         findViewById(R.id.ivConnectExtSig).setVisibility(View.VISIBLE);
         findViewById(R.id.tvPluginDevice).setVisibility(View.VISIBLE);
      }

      if (showTx){
         findViewById(R.id.ivConnectExtSig).setVisibility(View.GONE);
         findViewById(R.id.llShowTx).setVisibility(View.VISIBLE);

         ArrayList<String> toAddresses = new ArrayList<String>(1);

         long totalSending = 0;

         for (TransactionOutput o : _unsigned.getOutputs()){
            Address toAddress;
            toAddress = o.script.getAddress(_mbwManager.getNetwork());
            Optional<Integer[]> addressId = ((HDAccount) _account).getAddressId(toAddress);

            if (! (addressId.isPresent() && addressId.get()[0]==1) ){
               // this output goes to a foreign address (addressId[0]==1 means its internal change)
               totalSending += o.value;
               toAddresses.add(toAddress.toDoubleLineString());
            }
         }

         String toAddress = Joiner.on(",\n").join(toAddresses);
         String amount = CoinUtil.valueString(totalSending, false) + " BTC";
         String total = CoinUtil.valueString(totalSending + _unsigned.calculateFee(), false) + " BTC";
         String fee = CoinUtil.valueString(_unsigned.calculateFee(), false) + " BTC";

         ((TextView)findViewById(R.id.tvAmount)).setText(amount);
         ((TextView)findViewById(R.id.tvToAddress)).setText(toAddress);
         ((TextView)findViewById(R.id.tvFee)).setText(fee);
         ((TextView)findViewById(R.id.tvTotal)).setText(total);
      }
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event){
      MasterseedPasswordDialog pwd = new MasterseedPasswordDialog();
      pwd.show(getFragmentManager(), PASSPHRASE_FRAGMENT_TAG);
   }

   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event){
      Utils.showSimpleMessageDialog(ExtSigSignTransactionActivity.this, event.errorMessage, new Runnable() {
         @Override
         public void run() {
            ExtSigSignTransactionActivity.this.setResult(RESULT_CANCELED);
            // close this activity and let the user try again
            ExtSigSignTransactionActivity.this.finish();
         }
      });

      // kill the signing task
      ExtSigSignTransactionActivity.this.cancelSigningTask();
   }

   @Subscribe
   public void onPinMatrixRequest(ExternalSignatureDeviceManager.OnPinMatrixRequest event){
      TrezorPinDialog pin = new TrezorPinDialog(ExtSigSignTransactionActivity.this, true);
      pin.setOnPinValid(new PinDialog.OnPinEntered(){
         @Override
         public void pinEntered(PinDialog dialog, Pin pin) {
            getExtSigManager().enterPin(pin.getPin());
            dialog.dismiss();
         }
      });
      pin.show();

      // update the UI, as the state might have changed
      updateUi();
   }

   @Subscribe
   public void onButtonRequest(ExternalSignatureDeviceManager.OnButtonRequest event){
      showTx = true;
      updateUi();
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event){
      updateUi();
   }
}
