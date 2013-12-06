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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.crypto.Bip38.Bip38PrivateKey;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.service.Bip38KeyDecryptionTask;
import com.mycelium.wallet.service.ServiceTask;
import com.mycelium.wallet.service.ServiceTaskStatusEx;
import com.mycelium.wallet.service.ServiceTaskStatusEx.State;
import com.mycelium.wallet.service.TaskExecutionServiceController;
import com.mycelium.wallet.service.TaskExecutionServiceController.TaskExecutionServiceCallback;

public class DecryptBip38PrivateKeyActivity extends Activity implements TaskExecutionServiceCallback {

   private EditText passwordEdit;

   public static void callMe(Activity currentActivity, String encryptedPrivateKey, int requestCode) {
      Intent intent = new Intent(currentActivity, DecryptBip38PrivateKeyActivity.class);
      intent.putExtra("encryptedPrivateKey", encryptedPrivateKey);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Fragment fragment, String encryptedPrivateKey, int requestCode) {
      Intent intent = new Intent(fragment.getActivity(), DecryptBip38PrivateKeyActivity.class);
      intent.putExtra("encryptedPrivateKey", encryptedPrivateKey);
      fragment.startActivityForResult(intent, requestCode);
   }

   private TaskExecutionServiceController _taskExecutionServiceController;
   private ServiceTaskStatusEx _taskStatus;
   private ProgressUpdater _progressUpdater;
   private String _encryptedPrivateKey;
   private Bip38PrivateKey _bip38PrivateKey;
   private MbwManager _mbwManager;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.decrypt_bip38_private_key_activity);

      _mbwManager = MbwManager.getInstance(this);
      // Get parameters
      _encryptedPrivateKey = getIntent().getStringExtra("encryptedPrivateKey");

      // Decode the BIP38 key
      _bip38PrivateKey = Bip38.parseBip38PrivateKey(_encryptedPrivateKey);
      if (_bip38PrivateKey == null) {
         Toast.makeText(this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
         finish();
         return;
      }

      _progressUpdater = new ProgressUpdater();

      Button btDecrypt = (Button) findViewById(R.id.btDecrypt);
      btDecrypt.setEnabled(false);
      btDecrypt.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            startKeyStretching();
         }
      });

      passwordEdit = (EditText) findViewById(R.id.password);
      passwordEdit.addTextChangedListener(passwordWatcher);

      if (savedInstanceState != null) {
         String password = savedInstanceState.getString("password");
         if (password != null) {
            passwordEdit.setText(password);
         }
      }
      
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
       * Update the percentage of work completed for key stretching
       */
      @Override
      public void run() {

         if (_taskExecutionServiceController != null) {
            // poll for status update
            _taskExecutionServiceController.requestStatus();
         }

         if (_taskExecutionServiceController != null && _taskStatus != null) {
            ((TextView) findViewById(R.id.tvStatus)).setText(_taskStatus.statusMessage);
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (_taskStatus.progress * 100) + "%");
         } else {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
         }
         // Reschedule
         _handler.postDelayed(this, 300);
      }

   }

   private void startKeyStretching() {
      findViewById(R.id.btDecrypt).setEnabled(false);
      passwordEdit.setEnabled(false);
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.import_decrypt_stretching);
      ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.transparent));
      String password = ((EditText) findViewById(R.id.password)).getText().toString();

      if (_taskExecutionServiceController != null) {
         _taskExecutionServiceController.terminate();
         _taskExecutionServiceController.unbind(this);
      }
      _taskExecutionServiceController = new TaskExecutionServiceController();
      _taskExecutionServiceController.bind(this, this);
      Bip38KeyDecryptionTask task = new Bip38KeyDecryptionTask(_encryptedPrivateKey, password, this,
            _mbwManager.getNetwork());
      _taskExecutionServiceController.start(task);
   }

   @Override
   public void onStatusReceived(ServiceTaskStatusEx status) {
      if (_taskExecutionServiceController == null) {
         return;
      }
      _taskStatus = status;
      if (_taskStatus != null && _taskStatus.state == State.FINISHED) {
         _taskExecutionServiceController.requestResult();
      }
   }

   @Override
   public void onResultReceived(ServiceTask<?> task) {
      _progressUpdater.stop();

      try {
         String key = ((Bip38KeyDecryptionTask) task).getResult();
         if (key == null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
            ((TextView) findViewById(R.id.tvStatus)).setText("");
            Utils.showSimpleMessageDialog(this, R.string.import_decrypt_bip38_invalid_password);
         } else {
            // Success, return result
            Intent result = new Intent();
            result.putExtra("base58Key", key);
            setResult(RESULT_OK, result);
            this.finish();
         }
      } catch (OutOfMemoryError e) {
         ((TextView) findViewById(R.id.tvStatus)).setText(R.string.out_of_memory_error);
         ((TextView) findViewById(R.id.tvStatus)).setBackgroundColor(getResources().getColor(R.color.red));
         ((TextView) findViewById(R.id.tvProgress)).setText("");
         _mbwManager.reportIgnoredException(e);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

   }

}