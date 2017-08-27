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
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.UserFacingException;
import com.mycelium.wallet.activity.TextNormalizer;
import com.mycelium.wallet.service.MrdKeyStretchingTask;
import com.mycelium.wallet.service.ServiceTask;
import com.mycelium.wallet.service.ServiceTaskStatusEx;
import com.mycelium.wallet.service.ServiceTaskStatusEx.State;
import com.mycelium.wallet.service.TaskExecutionServiceController;
import com.mycelium.wallet.service.TaskExecutionServiceController.TaskExecutionServiceCallback;

import java.util.Locale;

public class MrdDecryptDataActivity extends Activity implements TaskExecutionServiceCallback {

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
      Intent intent = new Intent(currentActivity, MrdDecryptDataActivity.class);
      intent.putExtra("encryptedData", encryptedData);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Fragment fragment, String encryptedData, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), MrdDecryptDataActivity.class);
      intent.putExtra("encryptedData", encryptedData);
      fragment.startActivityForResult(intent, requestCode);
   }

   private TaskExecutionServiceController _taskExecutionServiceController;
   private ServiceTaskStatusEx _taskStatus;
   private boolean _oomDetected;
   private ProgressUpdater _progressUpdater;
   private String _encryptedData;
   private MbwManager _mbwManager;
   private MrdExport.V1.Header _header;
   private EncryptionParameters _encryptionParameters;

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
      _encryptedData = getIntent().getStringExtra("encryptedData");

      // Decode the header of the encrypted private key
      try {
         _header = MrdExport.V1.extractHeader(_encryptedData);
      } catch (DecodingException e) {
         Toast.makeText(this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
         finish();
         return;
      }

      _taskExecutionServiceController = new TaskExecutionServiceController();
      _taskExecutionServiceController.bind(this, this);

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
         findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.transparent));
      } else if (!MrdExport.isChecksumValid(getPassword())) {
         ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_key_invalid_checksum);
         findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.red));
      }
      // else Leave the status at what it is, it is updated by the progress

   }

   private void showKeyboardOrStartStretching() {
      _encryptionParameters = null;

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
      if (_taskExecutionServiceController != null) {
         _taskExecutionServiceController.terminate();
         _taskExecutionServiceController.unbind(this);
      }
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

         if (_oomDetected) {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
            ((TextView) findViewById(R.id.tvStatus)).setText(R.string.out_of_memory_error);
            return;
         }

         if (_taskStatus == null || _taskStatus.state == State.NOTRUNNING) {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
         } else if (_taskStatus.state == State.RUNNING) {
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (_taskStatus.progress * 100) + "%");
            ((TextView) findViewById(R.id.tvStatus)).setText(_taskStatus.statusMessage);

         }

         _taskExecutionServiceController.requestStatus();

         // Reschedule
         _handler.postDelayed(this, 300);
      }

   }

   private void startKeyStretching() {
      hideKeyboard();

      findViewById(R.id.tvStatus).setBackgroundColor(getResources().getColor(R.color.transparent));
      String password = getPassword().substring(0, MrdExport.V1.V1_PASSPHRASE_LENGTH);
      KdfParameters kdfParameters = KdfParameters.fromPassphraseAndHeader(password, _header);

      if (_taskExecutionServiceController != null) {
         _taskExecutionServiceController.terminate();
         _taskExecutionServiceController.unbind(this);
      }
      _taskExecutionServiceController = new TaskExecutionServiceController();
      _taskExecutionServiceController.bind(this, this);
      MrdKeyStretchingTask task = new MrdKeyStretchingTask(kdfParameters, this);
      _taskExecutionServiceController.start(task);
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
      if (_header.type == MrdExport.V1.Header.Type.UNCOMPRESSED ||
            _header.type == MrdExport.V1.Header.Type.COMPRESSED) {
         tryDecryptPrivateKey(parameters);
      } else if (_header.type == MrdExport.V1.Header.Type.MASTER_SEED) {
         tryDecryptMasterSeed(parameters);
      } else {
         Toast.makeText(this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
         finish();
      }
   }

   private void tryDecryptPrivateKey(EncryptionParameters parameters) {
      try {
         String key = MrdExport.V1.decryptPrivateKey(parameters, _encryptedData, _mbwManager.getNetwork());
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

   private void tryDecryptMasterSeed(EncryptionParameters parameters) {
      try {
         Bip39.MasterSeed masterSeed = MrdExport.V1.decryptMasterSeed(parameters, _encryptedData, _mbwManager.getNetwork());
         // Success, return result
         Intent result = new Intent();
         result.putExtra("masterSeed", masterSeed);
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
            MrdDecryptDataActivity.this.finish();
         }
      });

      AlertDialog alert = builder.create();
      alert.show();
   }

   @Override
   public void onStatusReceived(ServiceTaskStatusEx status) {
      _taskStatus = status;
      if (_taskStatus != null && _taskStatus.state == State.FINISHED && _encryptionParameters == null) {
         _taskExecutionServiceController.requestResult();
      }
   }

   @Override
   public void onResultReceived(ServiceTask<?> result) {
      //If we receive another service task having another type - just return
      if (!(result instanceof MrdKeyStretchingTask))
         return;

      _taskExecutionServiceController.terminate();
      ((TextView) findViewById(R.id.tvProgress)).setText("");

      MrdKeyStretchingTask task = (MrdKeyStretchingTask) result;
      try {
         _encryptionParameters = task.getResult();
         tryDecrypt(_encryptionParameters);
      } catch (UserFacingException e) {
         _oomDetected = true;
         _mbwManager.reportIgnoredException(e);
      }
   }

}
