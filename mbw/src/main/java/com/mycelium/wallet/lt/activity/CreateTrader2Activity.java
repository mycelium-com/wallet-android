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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.TryLogin;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * CreateTrader{1|2|3}Activity are a sort of Trader Account Creation Wizard.
 * You start at 1, go to 2, finish at 3.
 */
public class CreateTrader2Activity extends Activity {

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, CreateTrader2Activity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
      currentActivity.startActivity(intent);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private List<UUID> _accounts;
   private Spinner _spAddress;
   private Button _btUse;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_create_trader_2_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _spAddress = findViewById(R.id.spAddress);
      _btUse = findViewById(R.id.btUse);

      _btUse.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            _spAddress.setEnabled(false);
            _btUse.setEnabled(false);
            // Show progress bar while waiting
            findViewById(R.id.pbWait).setVisibility(View.VISIBLE);

            InMemoryPrivateKey privateKey = getSelectedPrivateKey();
            if (privateKey == null) {
               return;
            }
            _ltManager.makeRequest(new TryLogin(privateKey, _mbwManager.getNetwork()));
         }
      });

      // Populate account chooser, you can also choose archived accounts
      _accounts = new LinkedList<>();
      List<String> choices = new LinkedList<>();
      WalletManager walletManager = _mbwManager.getWalletManager(false);
      for (UUID accountId : walletManager.getUniqueIds()) {
         WalletAccount account = walletManager.getAccount(accountId);
         if (!account.canSpend()) {
            continue;
         }
         if (account instanceof HDAccount && !account.isDerivedFromInternalMasterseed()) {
            continue;
         }
         if (!Utils.isAllowedForLocalTrader(account)) {
            continue;
         }

         String name = _mbwManager.getMetadataStorage().getLabelByAccount(accountId);
         if (name.length() == 0) {
            name = createDefaultName(account);
         }
         choices.add(name);
         _accounts.add(accountId);
      }

      ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
      dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      _spAddress.setAdapter(dataAdapter);

      // Load saved state
      if (savedInstanceState != null) {
         int selectedIndex = savedInstanceState.getInt("selectedIndex", Spinner.INVALID_POSITION);
         if (selectedIndex != Spinner.INVALID_POSITION) {
            _spAddress.setSelection(selectedIndex);
         }
      }
      _btUse.setEnabled(_spAddress.getSelectedItemPosition() != Spinner.INVALID_POSITION);
   }

   private String createDefaultName(WalletAccount account) {
      if (account instanceof SingleAddressAccount) {
         Address address = ((SingleAddressAccount) account).getAddress();
         String addressString = address.toString();
         return addressString.substring(0, 6) +
                 "..." +
                 addressString.substring(addressString.length() - 6);
      } else {
         // We only get here if the user actively chose to have an empty account name
         // We default to the string representation of the account it
         return account.getId().toString();
      }
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putInt("selectedIndex", _spAddress.getSelectedItemPosition());
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      _ltManager.subscribe(ltSubscriber);
      if (_accounts.size() == 1) {
         // We have exactly one private key, use it automatically
         findViewById(R.id.pbWait).setVisibility(View.VISIBLE);
         findViewById(R.id.llRoot).setVisibility(View.GONE);
         WalletAccount account = _mbwManager.getWalletManager(false).getAccount(_accounts.get(0));
         InMemoryPrivateKey privateKey = _mbwManager.obtainPrivateKeyForAccount(account, LocalTraderManager.LT_DERIVATION_SEED, AesKeyCipher.defaultKeyCipher());
         _ltManager.makeRequest(new TryLogin(privateKey, _mbwManager.getNetwork()));
      } else {
         // Let the user choose which private key to use
         findViewById(R.id.pbWait).setVisibility(View.GONE);
         findViewById(R.id.llRoot).setVisibility(View.VISIBLE);
      }
      super.onResume();
   }

   @Override
   protected void onPause() {
      _ltManager.unsubscribe(ltSubscriber);
      super.onPause();
   }

   private InMemoryPrivateKey getSelectedPrivateKey() {
      int index = _spAddress.getSelectedItemPosition();
      if (index == Spinner.INVALID_POSITION) {
         return null;
      }
      UUID accountId = _accounts.get(index);
      WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountId);
      return _mbwManager.obtainPrivateKeyForAccount(account, LocalTraderManager.LT_DERIVATION_SEED, AesKeyCipher.defaultKeyCipher());
   }

   private UUID getSelectedAccount() {
      int index = _spAddress.getSelectedItemPosition();
      if (index == Spinner.INVALID_POSITION) {
         return null;
      }
      return _accounts.get(index);
   }

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {

      @Override
      public void onLtError(int errorCode) {
         Toast.makeText(CreateTrader2Activity.this, R.string.lt_error_api_occurred, Toast.LENGTH_LONG).show();
         finish();
      }

      @Override
      public boolean onNoLtConnection() {
         Utils.toastConnectionError(CreateTrader2Activity.this);
         finish();
         return true;
      }

      @Override
      public boolean onLtNoTraderAccount() {
         // No existing trader with this key, normal case.
         // Proceed to creation step 2
         InMemoryPrivateKey privateKey = Preconditions.checkNotNull(getSelectedPrivateKey());
         UUID accountId = Preconditions.checkNotNull(getSelectedAccount());
         CreateTrader3Activity.callMe(CreateTrader2Activity.this, accountId, privateKey);
         finish();
         return true;
      }

      @Override
      public void onLtLogin(String nickname, TryLogin request) {
         // We are already registered with this key
         InMemoryPrivateKey privateKey = Preconditions.checkNotNull(getSelectedPrivateKey());
         UUID accountId = Preconditions.checkNotNull(getSelectedAccount());
         _ltManager.setLocalTraderData(accountId, privateKey, privateKey.getPublicKey().toAddress(_mbwManager.getNetwork(), AddressType.P2PKH), nickname);
         setResult(RESULT_OK);
         finish();
      }
   };
}