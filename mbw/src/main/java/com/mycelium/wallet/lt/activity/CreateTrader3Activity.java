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

package com.mycelium.wallet.lt.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.lt.InputValidator;
import com.mycelium.lt.api.LtApi;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.CreateTrader;

import java.util.UUID;

/**
 * CreateTrader{1|2|3}Activity are a sort of Trader Account Creation Wizard.
 * You start at 1, go to 2, finish at 3.
 */
public class CreateTrader3Activity extends Activity {
   public static void callMe(Activity currentActivity, UUID accountId, InMemoryPrivateKey privateKey) {
      Intent intent = new Intent(currentActivity, CreateTrader3Activity.class)
              .putExtra("accountId", accountId)
              .putExtra("privateKey", privateKey)
              .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private EditText _etName;
   private Button _btCreate;
   private UUID _accountId;
   private InMemoryPrivateKey _privateKey;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_trader_3_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _etName = findViewById(R.id.etName);
      _btCreate = findViewById(R.id.btUse);

      _btCreate.setOnClickListener(createClickListener);

      // Load intent parameters
      _accountId = (UUID) Preconditions.checkNotNull(getIntent().getSerializableExtra("accountId"));
      _privateKey = (InMemoryPrivateKey) Preconditions.checkNotNull(getIntent().getSerializableExtra("privateKey"));

      // XXX load saved name
      enableDisableOk();

      _etName.addTextChangedListener(nameWatcher);
   }

   OnClickListener createClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         enableUi(false);
         // Show progress bar while waiting
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         // Try create trader
         _ltManager.makeRequest(new CreateTrader(_privateKey, getNickName(), _mbwManager.getLanguage(), _mbwManager
               .getNetwork()));
      }
   };

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      super.onResume();
   }

   @Override
   protected void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   private void enableUi(boolean enabled) {
      _etName.setEnabled(enabled);
      _btCreate.setEnabled(enabled);
   }

   TextWatcher nameWatcher = new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      @Override
      public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
      }

      @Override
      public void afterTextChanged(Editable editable) {
         enableDisableOk();
      }
   };

   private void enableDisableOk() {
      _btCreate.setEnabled(InputValidator.isValidTraderName(getNickName()));
   }

   private String getNickName() {
      Editable text = _etName.getText();
      if (text == null)
         return "";
      return text.toString();
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {
      @Override
      public void onLtError(int errorCode) {
         if (errorCode == LtApi.ERROR_CODE_TRADER_NICKNAME_NOT_UNIQUE) {
            // The name was not available, let the user choose another name
            Toast.makeText(CreateTrader3Activity.this, R.string.lt_error_account_name_taken, Toast.LENGTH_LONG).show();
            enableUi(true);
            findViewById(R.id.pbWait).setVisibility(View.GONE);
        } else {
            // Some other error
            Toast.makeText(CreateTrader3Activity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
            finish();
         }
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(CreateTrader3Activity.this);
         finish();
         return true;
      }

      @Override
      public void onLtTraderCreated(CreateTrader request) {
         // Create Trader Success
         // Hide progress bar
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         _ltManager.setLocalTraderData(_accountId, _privateKey, _privateKey.getPublicKey().toAddress(_mbwManager.getNetwork(), AddressType.P2PKH), getNickName()); // TODO fix
         setResult(RESULT_OK);
         finish();
      }
   };
}