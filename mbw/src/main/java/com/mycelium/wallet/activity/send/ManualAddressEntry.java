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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.UUID;

public class ManualAddressEntry extends Activity {

   public static final String ADDRESS_RESULT_NAME = "address";
   private Address _address;
   private String _entered;
   private MbwManager _mbwManager;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manual_entry);
      boolean isColdStorage = getIntent().getBooleanExtra(SendMainActivity.IS_COLD_STORAGE, false);
      UUID accountUUID = (UUID) getIntent().getSerializableExtra(SendMainActivity.ACCOUNT);

      _mbwManager = MbwManager.getInstance(this);
      ((EditText) findViewById(R.id.etAddress)).addTextChangedListener(textWatcher);
      findViewById(R.id.btOk).setOnClickListener(okClickListener);
      ((EditText) findViewById(R.id.etAddress)).setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

      WalletAccount account = _mbwManager.getWalletManager(isColdStorage).getAccount(accountUUID);
      if(account instanceof ColuAccount) {
         ColuAccount coluAccount = (ColuAccount) account;
         ((TextView) findViewById(R.id.title)).setText(getString(R.string.enter_address, coluAccount.getColuAsset().name));
         ((TextView) findViewById(R.id.tvBitcoinAddressValid)).setText(getString(R.string.address_valid, coluAccount.getColuAsset().name));
         ((TextView) findViewById(R.id.tvBitcoinAddressInvalid)).setText(getString(R.string.address_invalid, coluAccount.getColuAsset().name));
      }
      // Load saved state
      if (savedInstanceState != null) {
         _entered = savedInstanceState.getString("entered");
      } else {
         _entered = "";
      }

   }

   @Override
   protected void onResume() {
      ((EditText) findViewById(R.id.etAddress)).setText(_entered);
      super.onResume();
   }

   @Override
   public void onSaveInstanceState(Bundle savedInstanceState) {
      super.onSaveInstanceState(savedInstanceState);
      savedInstanceState.putSerializable("entered", ((EditText) findViewById(R.id.etAddress)).getText().toString());
   }

   OnClickListener okClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         Intent result = new Intent();
         result.putExtra(ADDRESS_RESULT_NAME, _address);
         ManualAddressEntry.this.setResult(RESULT_OK, result);
         ManualAddressEntry.this.finish();
      }
   };
   TextWatcher textWatcher = new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      @Override
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      @Override
      public void afterTextChanged(Editable editable) {
         _entered = editable.toString();
         _address = Address.fromString(_entered.trim(), _mbwManager.getNetwork());

         findViewById(R.id.btOk).setEnabled(_address != null);
         boolean addressValid = _address != null;
         findViewById(R.id.tvBitcoinAddressInvalid).setVisibility(!addressValid ? View.VISIBLE : View.GONE);
         findViewById(R.id.tvBitcoinAddressValid).setVisibility(addressValid ? View.VISIBLE : View.GONE);
         findViewById(R.id.btOk).setEnabled(addressValid);
      }
   };

}
