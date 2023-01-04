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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.TextNormalizer;
import com.mycelium.wallet.activity.modern.Toaster;
import com.squareup.otto.Subscribe;

import java.util.Locale;

public class MrdDecryptDataActivity extends AppCompatActivity {
   public static final CharMatcher LETTERS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));
   public static final Splitter SPLIT_3 = Splitter.fixedLength(3);
   public static final Joiner JOIN_SPACE = Joiner.on(' ');
   public static final Function<String, String> PASS_NORMALIZER = new Function<String, String>() {
      @Override
      public String apply(String input) {
         String onlyLetters = LETTERS.retainFrom(input);
         return JOIN_SPACE.join(SPLIT_3.split(onlyLetters)).toUpperCase(Locale.US);
      }
   };
   public static final int PASSLENGTH_WITHSPACES = 21;

   private EditText passwordEdit;

   public static void callMe(Activity currentActivity, String encryptedData, int requestCode) {
      Intent intent = new Intent(currentActivity, MrdDecryptDataActivity.class)
              .putExtra("encryptedData", encryptedData);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private String encryptedData;
   private MbwManager mbwManager;
   private MrdExport.V1.Header header;
   private EncryptionParameters encryptionParameters;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.export_decrypt_private_key_activity);

      mbwManager = MbwManager.getInstance(this);
      // Get parameters
      encryptedData = getIntent().getStringExtra("encryptedData");

      // Decode the header of the encrypted private key
      try {
         header = MrdExport.V1.extractHeader(encryptedData);
      } catch (DecodingException e) {
         new Toaster(this).toast(R.string.unrecognized_format, true);
         finish();
         return;
      }

      passwordEdit = findViewById(R.id.password);
      passwordEdit.addTextChangedListener(new TextNormalizer(PASS_NORMALIZER, passwordEdit));

      passwordEdit.addTextChangedListener(new TextWatcher() {
         @Override
         public void beforeTextChanged(CharSequence s, int start, int count, int after) {
         }

         @Override
         public void onTextChanged(CharSequence s, int start, int before, int count) {
         }

         @Override
         public void afterTextChanged(Editable s) {
            if (isValidPassword(s)) {
               startKeyStretching();
            }
            updatePasswordText();
         }
      });

      if (savedInstanceState != null) {
         setPassword(savedInstanceState.getString("password"));
      }

      updatePasswordText();

      showKeyboardOrStartStretching();
   }

   private void updatePasswordText() {
      if (getPassword().length() != MrdExport.V1.V1_PASSPHRASE_LENGTH + 1) {
         ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_key_enter_password);
         findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.transparent));
      } else if (!MrdExport.isChecksumValid(getPassword())) {
         ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_key_invalid_checksum);
         findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.red));
      }
      // else Leave the status at what it is, it is updated by the progress
   }

   private void showKeyboardOrStartStretching() {
      encryptionParameters = null;

      if (this.isFinishing()) {
         return;
      }
      if (isValidPassword(passwordEdit.getText())) {
         startKeyStretching();
      } else {
         showKeyboard();
      }
   }

   private boolean isValidPassword(CharSequence s) {
      return allCharsEntered(s) && MrdExport.isChecksumValid(getPassword());
   }

   private boolean allCharsEntered(CharSequence s) {
      return s.length() == PASSLENGTH_WITHSPACES;
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putString("password", getPassword());
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

   public String getPassword() {
      Editable text = passwordEdit.getText();
      if (text == null)
         return "";
      return LETTERS.retainFrom(text);
   }

   public void setPassword(String enteredText) {
      passwordEdit.setText(enteredText);
   }

   private void startKeyStretching() {
      hideKeyboard();

      findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.transparent));
      String password = getPassword().substring(0, MrdExport.V1.V1_PASSPHRASE_LENGTH);
      KdfParameters kdfParameters = KdfParameters.fromPassphraseAndHeader(password, header);
      MrdKeyStretchingTask keyStretchingTask = new MrdKeyStretchingTask(kdfParameters);
      keyStretchingTask.execute();
   }

   private void showKeyboard() {
      passwordEdit.requestFocus();
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInputFromInputMethod(passwordEdit.getWindowToken(), 0);
   }

   private void hideKeyboard() {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(passwordEdit.getWindowToken(), 0);
   }

   private void tryDecrypt(EncryptionParameters parameters) {
      if (header.type == MrdExport.V1.Header.Type.UNCOMPRESSED ||
              header.type == MrdExport.V1.Header.Type.COMPRESSED) {
         tryDecryptPrivateKey(parameters);
      } else if (header.type == MrdExport.V1.Header.Type.MASTER_SEED) {
         tryDecryptMasterSeed(parameters);
      } else {
         new Toaster(this).toast(R.string.unrecognized_format, true);
         finish();
      }
   }

   private void tryDecryptPrivateKey(EncryptionParameters parameters) {
      try {
         String key = MrdExport.V1.decryptPrivateKey(parameters, encryptedData, mbwManager.getNetwork());
         // Success, return result
         Intent result = new Intent()
                 .putExtra("base58Key", key)
                 .putExtra("encryptionParameters", parameters);
         setResult(RESULT_OK, result);
         finish();
      } catch (InvalidChecksumException e) {
         // Invalid password, ask the user if he wishes to retry
         showRetryDialog();
      } catch (DecodingException e) {
         new Toaster(this).toast(R.string.unrecognized_format, true);
         finish();
      }
   }

   private void tryDecryptMasterSeed(EncryptionParameters parameters) {
      try {
         Bip39.MasterSeed masterSeed = MrdExport.V1.decryptMasterSeed(parameters, encryptedData, mbwManager.getNetwork());
         // Success, return result
         Intent result = new Intent()
                 .putExtra("masterSeed", masterSeed)
                 .putExtra("encryptionParameters", parameters);
         setResult(RESULT_OK, result);
         finish();
      } catch (InvalidChecksumException e) {
         // Invalid password, ask the user if he wishes to retry
         showRetryDialog();
      } catch (DecodingException e) {
         new Toaster(this).toast(R.string.unrecognized_format, true);
         finish();
      }
   }

   private void showRetryDialog() {
      new AlertDialog.Builder(this)
              .setTitle(getString(R.string.invalid_password))
              .setMessage(getString(R.string.retry_password_question))
              .setPositiveButton(R.string.yes, (dialog, which) -> {
                 setPassword("");
                 updatePasswordText();
                 showKeyboardOrStartStretching();
                 dialog.dismiss();
              })
              .setNegativeButton(R.string.no, (dialog, which) -> {
                 dialog.dismiss();
                 finish();
              })
              .create()
              .show();
   }

   @Subscribe
   public void onResultReceived(MrdKeyStretchingResult result) {
      ((TextView) findViewById(R.id.tvProgress)).setText("");
      encryptionParameters = result.encryptionParameters;
      tryDecrypt(encryptionParameters);
   }

   private static class MrdKeyStretchingTask extends AsyncTask<Void, Void, EncryptionParameters> {
      private MrdExport.V1.KdfParameters kdfParameters;
      @StringRes
      private Integer statusMessageId;

      MrdKeyStretchingTask(MrdExport.V1.KdfParameters kdfParameters) {
         this.kdfParameters = kdfParameters;
         statusMessageId = R.string.import_decrypt_stretching;
      }

      @Override
      protected EncryptionParameters doInBackground(Void... voids) {
         // Generate Encryption parameters by doing key stretching
         EncryptionParameters encryptionParameters = null;
         try {
            encryptionParameters = EncryptionParameters.generate(kdfParameters);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

         } catch (OutOfMemoryError e) {
            statusMessageId = R.string.out_of_memory_error;
         }
         return encryptionParameters;
      }

      @Override
      protected void onPostExecute(EncryptionParameters encryptionParameters) {
         super.onPostExecute(encryptionParameters);
         if (encryptionParameters != null) {
            MbwManager.getEventBus().post(new MrdKeyStretchingResult(encryptionParameters));
         } else {
            MbwManager.getEventBus().post(new MrdKeyStretchingError(statusMessageId));
         }
      }
   }

   private static class MrdKeyStretchingResult {
      EncryptionParameters encryptionParameters;

      MrdKeyStretchingResult(EncryptionParameters encryptionParameters) {
         this.encryptionParameters = encryptionParameters;
      }
   }

   private static class MrdKeyStretchingError {
      @StringRes
      Integer statusMessageId;

      MrdKeyStretchingError(Integer statusMessageId) {
         this.statusMessageId = statusMessageId;
      }
   }
}
