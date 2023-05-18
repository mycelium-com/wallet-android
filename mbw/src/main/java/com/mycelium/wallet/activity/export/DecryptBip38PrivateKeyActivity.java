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

package com.mycelium.wallet.activity.export;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.*;

import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.crypto.Bip38.Bip38PrivateKey;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class DecryptBip38PrivateKeyActivity extends AppCompatActivity {
   private EditText passwordEdit;
   private CheckBox checkboxShowPassword;
   Bip38KeyDecryptionTask task;
   private String encryptedPrivateKey;
   private MbwManager mbwManager;

   public static void callMe(Activity currentActivity, String encryptedPrivateKey, int requestCode) {
      Intent intent = new Intent(currentActivity, DecryptBip38PrivateKeyActivity.class)
              .putExtra("encryptedPrivateKey", encryptedPrivateKey);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Fragment fragment, String encryptedPrivateKey, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), DecryptBip38PrivateKeyActivity.class)
              .putExtra("encryptedPrivateKey", encryptedPrivateKey);
      fragment.startActivityForResult(intent, requestCode);
   }

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.decrypt_bip38_private_key_activity);

      mbwManager = MbwManager.getInstance(this);
      // Get parameters
      encryptedPrivateKey = getIntent().getStringExtra("encryptedPrivateKey");

      // Decode the BIP38 key
      Bip38PrivateKey bip38Privatekey = Bip38.parseBip38PrivateKey(encryptedPrivateKey);
      if (bip38Privatekey == null) {
         new Toaster(this).toast(R.string.unrecognized_format, true);
         finish();
         return;
      }

      Button btDecrypt = findViewById(R.id.btDecrypt);
      btDecrypt.setEnabled(false);
      btDecrypt.setOnClickListener(v -> startKeyStretching());

      passwordEdit = findViewById(R.id.password);
      passwordEdit.addTextChangedListener(passwordWatcher);

      checkboxShowPassword = findViewById(R.id.showPassword);
      checkboxShowPassword.setOnCheckedChangeListener((compoundButton, b) -> setPasswordHideShow( checkboxShowPassword.isChecked() ));

      if (savedInstanceState != null) {
         String password = savedInstanceState.getString("password");
         if (password != null) {
            passwordEdit.setText(password);
         }
      }
   }

   private void setPasswordHideShow(boolean show){
      if (show){
         // Show password in plaintext
         passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
      }else{
         // Hide password
         passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
      }
      // Set cursor to last position
      passwordEdit.setSelection(passwordEdit.getText().length());
   }

   TextWatcher passwordWatcher = new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
         findViewById(R.id.btDecrypt).setEnabled(s.length() > 0);
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
   };

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putString("password", passwordEdit.getText().toString());
      super.onSaveInstanceState(outState);
   }

   @Override
   protected void onResume() {
      MbwManager.getEventBus().register(this);
      super.onResume();
   }

   @Override
   protected void onPause() {
      MbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   private void startKeyStretching() {
      findViewById(R.id.btDecrypt).setEnabled(false);
      checkboxShowPassword.setChecked(false);
      passwordEdit.setEnabled(false);
      checkboxShowPassword.setEnabled(false);

      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_stretching);
      findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.transparent));
      String password = ((EditText) findViewById(R.id.password)).getText().toString();

      task = new Bip38KeyDecryptionTask(encryptedPrivateKey, password, mbwManager.getNetwork());
      task.execute();
   }

   @Subscribe
   public void onResultReceived(Bip38KeyDecryptionResult decryptionResult) {
      String key = decryptionResult.result;
      Intent result = new Intent()
              .putExtra("base58Key", key);
      setResult(RESULT_OK, result);
      finish();
   }

   @Subscribe
   public void onErrorReceived(Bip38KeyDecryptionError decryptionError) {
      TextView tvStatus = findViewById(R.id.tvStatus);
      ((TextView) findViewById(R.id.tvProgress)).setText("");
      Integer decryptionMessageId = decryptionError.message;
      if (decryptionMessageId == R.string.import_decrypt_bip38_invalid_password) {
         tvStatus.setText("");
         Utils.showSimpleMessageDialog(this, R.string.import_decrypt_bip38_invalid_password);
         passwordEdit.setEnabled(true);
         checkboxShowPassword.setEnabled(true);
      } else {
         tvStatus.setText(decryptionMessageId);
         tvStatus.setBackgroundColor(getResources().getColor(R.color.red));
      }
   }

   private static class Bip38KeyDecryptionTask extends AsyncTask<Void, Void, String> {
      private String bip38PrivateKeyString;
      private String passphrase;
      private NetworkParameters network;
      @StringRes
      private Integer statusMessageId;

      Bip38KeyDecryptionTask(String bip38PrivateKeyString, String passphrase, NetworkParameters network) {
         this.bip38PrivateKeyString = bip38PrivateKeyString;
         this.passphrase = passphrase;
         this.network = network;
         this.statusMessageId = R.string.import_decrypt_stretching;
      }

      @Override
      protected String doInBackground(Void ... voids) {
         // Do BIP38 decryption
         String result;
         try {
            result = Bip38.decrypt(bip38PrivateKeyString, passphrase, network);
            if (result == null) {
               statusMessageId = R.string.import_decrypt_bip38_invalid_password;
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statusMessageId = null;
            return null;
         } catch (OutOfMemoryError e) {
            statusMessageId = R.string.out_of_memory_error;
            return null;
         }
         // The result may be null
         return result;
      }

      @Override
      protected void onPostExecute(String result) {
         Bus bus = MbwManager.getEventBus();
         if (result != null) {
            bus.post(new Bip38KeyDecryptionResult(result));
         } else {
            bus.post(new Bip38KeyDecryptionError(statusMessageId));
         }
      }
   }

   private static class Bip38KeyDecryptionResult {
      String result;

      Bip38KeyDecryptionResult(String result) {
         this.result = result;
      }
   }

   private static class Bip38KeyDecryptionError {
      @StringRes
      Integer message;

      Bip38KeyDecryptionError(int message) {
         this.message = message;
      }
   }
}