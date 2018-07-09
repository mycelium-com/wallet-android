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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.colu.ColuAccount.ColuAsset;
import com.mycelium.wallet.colu.ColuAccount.ColuAssetType;
import com.mycelium.wallet.colu.ColuManager;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.wallet.SyncMode;
import com.squareup.otto.Bus;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;
import static com.mycelium.wallet.colu.ColuAccount.ColuAssetType.MASS;
import static com.mycelium.wallet.colu.ColuAccount.ColuAssetType.MT;
import static com.mycelium.wallet.colu.ColuAccount.ColuAssetType.RMC;

public class AddColuAccountActivity extends Activity {
   public static final int RESULT_COLU = 3;

   public static final String TAG = "AddColuAccountActivity";

   @BindView(R.id.btColuAddAccount) Button btColuAddAccount;
//   @BindView(R.id.tvTosLink) TextView tvTosLink;

   ColuAsset selectedColuAsset;

   public static Intent getIntent(Context context) {
      Intent intent = new Intent(context, AddColuAccountActivity.class);
      return intent;
   }

   public static final String RESULT_KEY = "account";
   private MbwManager _mbwManager;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_colu_account_activity);
      _mbwManager = MbwManager.getInstance(this);
      ButterKnife.bind(this);
      btColuAddAccount.setText(getString(R.string.colu_create_account, ""));
//      setTosLink(tvTosLink);
   }

   void setButtonEnabled(){
         btColuAddAccount.setEnabled(true);
   }

//   private void setTosLink(TextView link) {
//      link.setClickable(true);
//      link.setMovementMethod(LinkMovementMethod.getInstance());
//      String linkUrl = getString(R.string.colu_tos_link_url);
//      String text = "<a href='" + linkUrl + "'> " + link.getText() + "</a>";
//      link.setText(Html.fromHtml(text));
//   }

   @OnClick(R.id.btColuAddAccount)
   void onColuAddAccountClick() {
      if(selectedColuAsset != null) {
         createColuAccountProtected(selectedColuAsset);
      } else {
         Toast.makeText(AddColuAccountActivity.this, R.string.colu_select_an_account_type, Toast.LENGTH_SHORT).show();
      }
     //displayTemporaryMessage();
   }

   @OnClick({R.id.radio_mycelium_tokens, R.id.radio_mass_tokens, R.id.radio_rmc_tokens})
   public void onRadioButtonClicked(View view) {
      // Is the button now checked?
      boolean checked = ((RadioButton) view).isChecked();
      ColuAssetType assetType;
      String name;
      // Check which radio button was clicked
      switch (view.getId()) {
         case R.id.radio_mycelium_tokens:
            assetType = MT;
            name = "MT";
            break;
         case R.id.radio_mass_tokens:
            assetType = MASS;
            name = "Mass";
            break;
         case R.id.radio_rmc_tokens:
            assetType = RMC;
            name = "RMC";
            break;
         default:
            return;
      }
      if (checked) {
         selectedColuAsset = ColuAsset.getByType(assetType);
      }
      btColuAddAccount.setEnabled(true);
      Toast.makeText(this, name + " selected", Toast.LENGTH_SHORT).show();
   }

   private void createColuAccountProtected(final ColuAsset coluAsset) {
      _mbwManager.getVersionManager().showFeatureWarningIfNeeded(
            AddColuAccountActivity.this, Feature.COLU_NEW_ACCOUNT, true, new Runnable() {
               @Override
               public void run() {
                  _mbwManager.runPinProtectedFunction(AddColuAccountActivity.this, new Runnable() {
                     @Override
                     public void run() {
                        createColuAccount(coluAsset);
                     }
                  });
               }
            }
      );
   }

   private void createColuAccount(final ColuAsset coluAsset) {

//      AlertDialog.Builder b = new AlertDialog.Builder(this);
//      b.setTitle(getString(R.string.colu));
//      View diaView = getLayoutInflater().inflate(R.layout.ext_colu_tos, null);
//      b.setView(diaView);
//      b.setPositiveButton(getString(R.string.agree), new DialogInterface.OnClickListener() {
//         @Override
//         public void onClick(DialogInterface dialog, int which) {
//            // Create the account initially without set email address
//            // if needed, the user can later set and verify it via account menu.
//            // for now we hard code asset = MT
       new AddColuAsyncTask(_mbwManager.getEventBus(), Optional.<String>absent(), coluAsset).execute();
//         }
//      });
//      b.setNegativeButton(getString(R.string.dontagree), null);
//
//      AlertDialog dialog = b.create();
//
//      dialog.show();
   }

   private class AddColuAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private final boolean alreadyHadColuAccount;
      private Bus bus;
      private final ColuAsset coluAsset;
      private ColuManager coluManager;
      private final ProgressDialog progressDialog;

      public AddColuAsyncTask(Bus bus, Optional<String> mail, ColuAsset coluAsset) {
         this.bus = bus;
         this.coluAsset = coluAsset;
         this.alreadyHadColuAccount = _mbwManager.getMetadataStorage().isPairedService(MetadataStorage.PAIRED_SERVICE_COLU);
         progressDialog = ProgressDialog.show(AddColuAccountActivity.this, getString(R.string.colu), getString(R.string.colu_creating_account, coluAsset.label));
         progressDialog.setCancelable(false);
         progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progressDialog.show();
      }

      @Override
      protected UUID doInBackground(Void... params) {
         _mbwManager.getMetadataStorage().setPairedService(MetadataStorage.PAIRED_SERVICE_COLU, true);
         coluManager = _mbwManager.getColuManager();
         if(coluManager == null) {
            Log.d(TAG, "Error could not obtain coluManager !");
            return null;
         } else {
            try {
               UUID uuid = coluManager.enableAsset(coluAsset, null);
               coluManager.scanForAccounts(SyncMode.FULL_SYNC_ALL_ACCOUNTS);
               return uuid;
            } catch (Exception e) {
               Log.d(TAG, "Error while creating Colored Coin account for asset " + coluAsset.name + ": " + e.getMessage());
               return null;
            }
         }
      }

      @Override
      protected void onPostExecute(UUID account) {
         if (account != null) {
            _mbwManager.addExtraAccounts(coluManager);
            bus.post(new AccountChanged(account));
            Intent result = new Intent();
            result.putExtra(RESULT_KEY, account);
            setResult(RESULT_OK, result);
            finish();
         } else {
            // something went wrong - clean up the half ready coluManager
            Toast.makeText(AddColuAccountActivity.this, R.string.colu_unable_to_create_account, Toast.LENGTH_SHORT).show();
            _mbwManager.getMetadataStorage().setPairedService(MetadataStorage.PAIRED_SERVICE_COLU, alreadyHadColuAccount);
         }
         if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
         }
         setButtonEnabled();
      }
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      setButtonEnabled();
      super.onResume();
   }


   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      _mbwManager.getVersionManager().closeDialog();
      super.onPause();
   }

}
