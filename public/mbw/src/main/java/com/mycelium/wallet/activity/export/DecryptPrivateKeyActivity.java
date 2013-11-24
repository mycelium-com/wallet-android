/*
 * Copyright 2013 Megion Research and Development GmbH
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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.TextNormalizer;
import com.mycelium.wallet.service.KeyStretcherService;
import com.mycelium.wallet.service.KeyStretcherService.Status;
import com.mycelium.wallet.service.KeyStretcherServiceController;
import com.mycelium.wallet.service.KeyStretcherServiceController.KeyStretcherServiceCallback;

public class DecryptPrivateKeyActivity extends Activity implements KeyStretcherServiceCallback {

   public static final CharMatcher LETTERS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z'));
   public static final Splitter SPLIT_3 = Splitter.fixedLength(3);
   public static final Joiner JOIN_SPACE = Joiner.on(' ');
   public static final Function<String, String> PASS_NORMALIZER = new Function<String, String>() {
      @Override
      public String apply(String input) {
         String onlyLetters = LETTERS.retainFrom(input);
         return JOIN_SPACE.join(SPLIT_3.split(onlyLetters)).toUpperCase();
      }
   };
   public static final int PASSLENGTH_WITHSPACES = 21;

   private EditText passwordEdit;

   public static void callMe(Activity currentActivity, String encryptedPrivateKey, int requestCode) {
      Intent intent = new Intent(currentActivity, DecryptPrivateKeyActivity.class);
      intent.putExtra("encryptedPrivateKey", encryptedPrivateKey);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Fragment fragment, String encryptedPrivateKey, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), DecryptPrivateKeyActivity.class);
      intent.putExtra("encryptedPrivateKey", encryptedPrivateKey);
      fragment.startActivityForResult(intent, requestCode);
   }

   private KeyStretcherServiceController _keyStretcherServiceController;
   private KeyStretcherService.Status _stretchingStatus;
   private ProgressUpdater _progressUpdater;
   private String _encryptedPrivateKey;
   private MbwManager _mbwManager;
   private MrdExport.V1.Header _header;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.export_decrypt_private_key_activity);

      _mbwManager = MbwManager.getInstance(this);
      // Get parameters
      _encryptedPrivateKey = getIntent().getStringExtra("encryptedPrivateKey");

      // Decode the header of the encrypted private key
      try {
         _header = MrdExport.V1.extractHeader(_encryptedPrivateKey);
      } catch (DecodingException e) {
         Toast.makeText(this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
         finish();
         return;
      }

      _progressUpdater = new ProgressUpdater();

      passwordEdit = (EditText) findViewById(R.id.password);
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
         ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.transparent));
      } else if (MrdExport.isChecksumValid(getPassword())) {
         // Leave the status at what it is, it is updated by the progress
      } else {
         ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_key_invalid_checksum);
         ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.red));
      }

   }

   private void showKeyboardOrStartStretching() {
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
      _progressUpdater.start();
      super.onResume();
   }

   @Override
   protected void onPause() {
      _progressUpdater.stop();
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      if (_keyStretcherServiceController != null)
         _keyStretcherServiceController.terminate();
      super.onDestroy();
   }

   public String getPassword() {
      Editable text = passwordEdit.getText();
      if (text == null)
         return "";
      return LETTERS.retainFrom(text);
   }

   public void setPassword(String _enteredText) {
      passwordEdit.setText(_enteredText);
   }

   class ProgressUpdater implements Runnable {
      Handler _handler;

      ProgressUpdater() {
         _handler = new Handler();
      }

      public void start() {
         _handler.post(this);
      }

      public void stop() {
         _handler.removeCallbacks(this);
      }

      /**
       * Update the percentage of work completed for key stretching and pdf
       * generation
       */
      @Override
      public void run() {

         if (_keyStretcherServiceController != null) {
            // poll for status update
            _keyStretcherServiceController.requestStatus();
         }

         if (_keyStretcherServiceController != null && _stretchingStatus != null) {
            if (_stretchingStatus.error) {
               ((TextView) findViewById(R.id.tvStatus)).setText(R.string.out_of_memory_error);
               ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.red));
               ((TextView) findViewById(R.id.tvProgress)).setText("");
            } else {
               ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (_stretchingStatus.progress * 100)
                     + "%");
            }

         } else {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
         }
         // Reschedule
         _handler.postDelayed(this, 300);
      }

   }

   private void startKeyStretching() {
      hideKeyboard();

      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_stretching);
      ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.transparent));
      String password = getPassword().substring(0, MrdExport.V1.V1_PASSPHRASE_LENGTH);
      KdfParameters kdfParameters = KdfParameters.fromPassphraseAndHeader(password, _header);

      if (_keyStretcherServiceController != null) {
         _keyStretcherServiceController.terminate();
      }
      _keyStretcherServiceController = new KeyStretcherServiceController();
      _keyStretcherServiceController.bind(this, this);
      _keyStretcherServiceController.start(kdfParameters);
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

   private void tryDecrypt(MrdExport.V1.EncryptionParameters parameters) {
      try {
         String key = MrdExport.V1.decrypt(parameters, _encryptedPrivateKey, _mbwManager.getNetwork());
         // Success, return result
         Intent result = new Intent();
         result.putExtra("base58Key", key);
         result.putExtra("encryptionParameters", parameters);
         setResult(RESULT_OK, result);
         this.finish();
      } catch (InvalidChecksumException e) {
         // Invalid password, ask the user if he wishes to retry
         showRetryDialog();
      } catch (DecodingException e) {
         Toast.makeText(this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
         finish();
      }
   }

   private void showRetryDialog() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);

      builder.setTitle(getString(R.string.invalid_password));
      builder.setMessage(getString(R.string.retry_password_question));

      builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface dialog, int which) {
            setPassword("");
            updatePasswordText();
            showKeyboardOrStartStretching();
            dialog.dismiss();
         }

      });

      builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

         @Override
         public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            DecryptPrivateKeyActivity.this.finish();
         }
      });

      AlertDialog alert = builder.create();
      alert.show();
   }

   @Override
   public void onStatusReceived(Status status) {
      if(_keyStretcherServiceController == null){
         return;
      }
      _stretchingStatus = status;
      if (_stretchingStatus.hasResult) {
         _keyStretcherServiceController.requestResult();
      }
   }

   @Override
   public void onResultReceived(EncryptionParameters parameters) {
      if(_keyStretcherServiceController == null){
         return;
      }
      _keyStretcherServiceController.terminate();
      _keyStretcherServiceController = null;
      tryDecrypt(parameters);
   }

}