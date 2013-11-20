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
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.export.BackgroundKeyStretcher.PostRunner;

public class DecryptPrivateKeyActivity extends Activity {

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

   private String _enteredText;
   private boolean _waitingForInput;
   private BackgroundKeyStretcher _stretcher;
   private ProgressUpdater _progressUpdater;
   private String _encryptedPrivateKey;
   private MbwManager _mbwManager;
   private MrdExport.V1.Header _header;

   /** Called when the activity is first created. */
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

      if (savedInstanceState != null) {
         _enteredText = savedInstanceState.getString("password");
      }
      if (_enteredText == null) {
         _enteredText = "";
      }

      updatePasswordText();

      _progressUpdater = new ProgressUpdater();
      _stretcher = new BackgroundKeyStretcher();
      // Make the root focusable otherwise we cannot force show the keyboard
      View myView = findViewById(android.R.id.content);
      myView.setFocusable(true);
      myView.setFocusableInTouchMode(true);

      // Start slightly delayed, otherwise we cannot force show the keyboard
      new Handler().postDelayed(new Runnable() {

         @Override
         public void run() {
            showKeyboardOrStartStretching();
         }
      }, 500);
   }

   private void updatePasswordText() {
      ((TextView) findViewById(R.id.tvPassword)).setText(getPasswordText(_enteredText));
      ((TextView) findViewById(R.id.tvChecksum)).setText(getChecksumText(_enteredText));
      if (_enteredText.length() != MrdExport.V1.V1_PASSPHRASE_LENGTH + 1) {
         ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_key_enter_password);
         ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.transparent));
      } else if (isChecksumValid(_enteredText)) {
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
      if (_enteredText.length() == MrdExport.V1.V1_PASSPHRASE_LENGTH + 1 && isChecksumValid(_enteredText)) {
         startKeyStretching();
      } else {
         showKeyboard();
      }
   }

   private static boolean isChecksumValid(String enteredText) {
      if (enteredText.length() != MrdExport.V1.V1_PASSPHRASE_LENGTH + 1) {
         return false;
      }
      String password = enteredText.substring(0, MrdExport.V1.V1_PASSPHRASE_LENGTH);
      char chechsumChar = MrdExport.V1.calculatePasswordChecksum(password);
      return Character.toUpperCase(chechsumChar) == enteredText.charAt(MrdExport.V1.V1_PASSPHRASE_LENGTH);
   }

   private void showKeyboard() {
      _waitingForInput = true;
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.showSoftInput(findViewById(android.R.id.content), InputMethodManager.SHOW_FORCED);
   }

   private void hideKeyboard() {
      _waitingForInput = false;
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putString("password", _enteredText);
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
      _stretcher.terminate();
      super.onDestroy();
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      if (!_waitingForInput) {
         return super.onKeyDown(keyCode, event);
      }

      if (keyCode == KeyEvent.KEYCODE_DEL) {
         if (_enteredText.length() > 0) {
            _enteredText = _enteredText.substring(0, _enteredText.length() - 1);
            updatePasswordText();
         }
         return super.onKeyDown(keyCode, event);
      }

      if (_enteredText.length() >= MrdExport.V1.V1_PASSPHRASE_LENGTH + 1) {
         // This shouldn't really happen, but we check just in case.
         return super.onKeyDown(keyCode, event);
      }

      int c = event.getUnicodeChar();
      if (c >= 'a' && c <= 'z') {
         char cc = (char) ('A' + (c - 'a'));
         _enteredText = _enteredText + cc;
      } else if (c >= 'A' && c <= 'Z') {
         char cc = (char) c;
         _enteredText = _enteredText + cc;
      }
      updatePasswordText();
      if (_enteredText.length() == MrdExport.V1.V1_PASSPHRASE_LENGTH + 1 && isChecksumValid(_enteredText)) {
         hideKeyboard();
         startKeyStretching();
      }
      return super.onKeyDown(keyCode, event);
   }

   private static String getPasswordText(String password) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < MrdExport.V1.V1_PASSPHRASE_LENGTH; i++) {
         if (i < password.length()) {
            sb.append(password.charAt(i));
         } else {
            sb.append('_');
         }
         if (i % 3 == 2) {
            sb.append(' ');
         }
      }
      return sb.toString();
   }

   private static String getChecksumText(String password) {
      if (password.length() < MrdExport.V1.V1_PASSPHRASE_LENGTH + 1) {
         return "  _";
      } else {
         return "  " + password.charAt(MrdExport.V1.V1_PASSPHRASE_LENGTH);
      }
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
         // Copy reference so we don't risk loosing it
         KdfParameters tracker = _stretcher.getProgressTracker();

         if (tracker != null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (tracker.getProgress() * 100) + "%");
         } else {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
         }

         // Reschedule
         _handler.postDelayed(this, 300);
      }

   }

   private void startKeyStretching() {
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_stretching);
      ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.transparent));
      String password = _enteredText.substring(0, MrdExport.V1.V1_PASSPHRASE_LENGTH);
      KdfParameters kdfParameters = KdfParameters.fromPassphraseAndHeader(password, _header);
      _stretcher.start(kdfParameters, new PostRunner() {

         @Override
         public void onPostExecute(boolean error, EncryptionParameters parameters) {
            if (error) {
               ((TextView) findViewById(R.id.tvStatus)).setText(R.string.out_of_memory_error);
               ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.red));
            } else {
               tryDecrypt(parameters);
            }
         }
      });
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

      builder.setTitle("Invalid Password");
      builder.setMessage("The password you entred is not valid.\nDo you wish to try again?");

      builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface dialog, int which) {
            _enteredText = "";
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

}