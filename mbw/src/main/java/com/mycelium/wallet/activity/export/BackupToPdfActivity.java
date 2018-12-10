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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.*;
import com.mycelium.wallet.service.CreateMrdBackupTask;
import com.mycelium.wallet.service.ServiceTask;
import com.mycelium.wallet.service.ServiceTaskStatusEx;
import com.mycelium.wallet.service.ServiceTaskStatusEx.State;
import com.mycelium.wallet.service.TaskExecutionServiceController;
import com.mycelium.wallet.service.TaskExecutionServiceController.TaskExecutionServiceCallback;
import com.mycelium.wapi.wallet.AesKeyCipher;

//todo HD: export master seed without address/xpub extra data.
//todo HD: later: be compatible with a common format
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.text.format.DateFormat.getDateFormat;

public class BackupToPdfActivity extends Activity implements TaskExecutionServiceCallback {
   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, BackupToPdfActivity.class);
      currentActivity.startActivity(intent);
   }

   private static final String FILE_NAME_PREFIX = "mycelium-backup";

   private static final int SHARE_REQUEST_CODE = 1;

   private MbwManager _mbwManager;
   private long _backupTime;
   private String _fileName;
   private String _password;
   private ProgressUpdater _progressUpdater;
   private TaskExecutionServiceController _taskExecutionServiceController;
   private ServiceTaskStatusEx _taskStatus;
   private boolean _isPdfGenerated;
   private boolean _oomDetected;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.export_to_pdf_activity);
      Utils.preventScreenshots(this);

      _mbwManager = MbwManager.getInstance(this.getApplication());

      // Load saved state
      if (savedInstanceState != null) {
         _backupTime = savedInstanceState.getLong("backupTime", 0);
         _password = savedInstanceState.getString("password");
         _isPdfGenerated = savedInstanceState.getBoolean("isPdfGenerated");
      }

      if (_backupTime == 0) {
         _backupTime = new Date().getTime();
      }

      if (_password == null) {
         _password = MrdExport.V1.generatePassword(new AndroidRandomSource()).toUpperCase(Locale.US);
      }

      _fileName = getExportFileName(_backupTime);
      _taskExecutionServiceController = new TaskExecutionServiceController();

      // Populate Password
      ((TextView) findViewById(R.id.tvPassword)).setText(splitPassword(_password));

      // Populate Checksum
      char checksumChar = MrdExport.V1.calculatePasswordChecksum(_password);
      String checksumString = ("  " + checksumChar).toUpperCase(Locale.US);
      ((TextView) findViewById(R.id.tvChecksum)).setText(checksumString);

      findViewById(R.id.btSharePdf).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View arg0) {
            sharePdf();
         }

      });

      findViewById(R.id.btVerify).setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            VerifyBackupActivity.callMe(BackupToPdfActivity.this);
         }
      });

      _progressUpdater = new ProgressUpdater();
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putLong("backupTime", _backupTime);
      outState.putString("password", _password);
      outState.putBoolean("isPdfGenerated", _isPdfGenerated);
      super.onSaveInstanceState(outState);
   }

   private String splitPassword(String password) {
      StringBuilder sb = new StringBuilder();
      boolean first = true;
      for (int i = 0; i < password.length(); i++) {
         if (first) {
            first = false;
         } else {
            if (i % 3 == 0) {
               sb.append(' ');
            }
         }
         sb.append(password.charAt(i));
      }
      return sb.toString();
   }

   @Override
   protected void onResume() {
      if (!_isPdfGenerated) {
         startTask();
      } else {
         enableSharing();
      }

      _progressUpdater.start();
      super.onResume();
   }

   class ProgressUpdater implements Runnable {
      final Handler _handler;

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

         if (_isPdfGenerated) {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
            ((TextView) findViewById(R.id.tvStatus)).setText(R.string.encrypted_pdf_backup_document_ready);
            return;
         }

         if (_taskStatus == null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
            ((TextView) findViewById(R.id.tvStatus)).setText("");
            _taskExecutionServiceController.requestStatus();
         } else {
            if (_taskStatus.state != State.FINISHED) {
               _taskExecutionServiceController.requestStatus();
            }
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (_taskStatus.progress * 100) + "%");
            ((TextView) findViewById(R.id.tvStatus)).setText(_taskStatus.statusMessage);
         }

         // Reschedule
         _handler.postDelayed(this, 300);
      }
   }

   @Override
   protected void onPause() {
      _progressUpdater.stop();
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      _taskExecutionServiceController.terminate();
      _taskExecutionServiceController.unbind(this);
      super.onDestroy();
   }

   private String getExportFileName(long exportTime) {
      Date exportDate = new Date(exportTime);
      Locale locale = getResources().getConfiguration().locale;
      String hourString = DateFormat.getDateInstance(DateFormat.SHORT, locale).format(exportDate);
      hourString = replaceInvalidFileNameChars(hourString);
      String dateString = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(exportDate);
      dateString = replaceInvalidFileNameChars(dateString);
      return FILE_NAME_PREFIX + '-' + hourString + '-' + dateString + ".pdf";
   }

   private static String replaceInvalidFileNameChars(String name) {
      return name
              .replace(':', '.')
              .replace(' ', '-')
              .replace('\\', '-')
              .replace('/', '-')
              .replace('*', '-')
              .replace('?', '-')
              .replace('"', '-')
              .replace('\'', '-')
              .replace('<', '-')
              .replace('>', '-')
              .replace('|', '-');
   }

   private String getFullExportFilePath() {
      return _fileName;
   }

   private void startTask() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      findViewById(R.id.btVerify).setEnabled(false);
      KdfParameters kdfParameters = KdfParameters.createNewFromPassphrase(_password, new AndroidRandomSource(),
            _mbwManager.getDeviceScryptParameters());
      CreateMrdBackupTask task = new CreateMrdBackupTask(kdfParameters, this.getApplicationContext(),
            _mbwManager.getWalletManager(false), AesKeyCipher.defaultKeyCipher(), _mbwManager.getMetadataStorage(),
            _mbwManager.getNetwork(), getFullExportFilePath());
      _taskExecutionServiceController.bind(this, this);
      _taskExecutionServiceController.start(task);
   }

   private void enableSharing() {
      findViewById(R.id.btSharePdf).setEnabled(true);
      findViewById(R.id.btVerify).setEnabled(true);
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.encrypted_pdf_backup_document_ready);
   }

   private void sharePdf() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      ((TextView) findViewById(R.id.tvStatus)).setText(getResources().getString(R.string.encrypted_pdf_backup_sharing));
      Uri uri = getUri();

      String bodyText = getResources().getString(R.string.encrypted_pdf_backup_email_text);
      Intent intent = ShareCompat.IntentBuilder.from(this).setStream(uri)
            // uri from FileProvider
            .setType("application/pdf").setSubject(getSubject()).setText(bodyText).getIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      grantPermissions(intent, uri);
      startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.share_with)),
            SHARE_REQUEST_CODE);
   }

   private String getSubject() {
      Date now = new Date();
      Context appContext = Preconditions.checkNotNull(getApplicationContext());
      DateFormat dateFormat = getDateFormat(appContext);
      return getResources().getString(R.string.encrypted_pdf_backup_email_title) + " " + dateFormat.format(now);
   }

   private void grantPermissions(Intent intent, Uri uri) {
      // grant permissions for all apps that can handle given intent
      // if we know of any malicious app that tries to intercept this,
      // we could block it here based on packageName
      PackageManager packageManager = Preconditions.checkNotNull(getPackageManager());
      List<ResolveInfo> resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
      for (ResolveInfo resolveInfo : resInfoList) {
         String packageName = resolveInfo.activityInfo.packageName;
         grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      }
   }

   private Uri getUri() {
      String authority = getFileProviderAuthority();
      return FileProvider.getUriForFile(this, authority, getFileStreamPath(getFullExportFilePath()));
   }

   // ignore null checks in this method
   private String getFileProviderAuthority() {
      try {
         PackageManager packageManager = getApplication().getPackageManager();
         PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_PROVIDERS);
         for (ProviderInfo info : packageInfo.providers) {
            if (info.name.equals("android.support.v4.content.FileProvider")) {
               return info.authority;
            }
         }
      } catch (NameNotFoundException e) {
         throw new RuntimeException(e);
      }
      throw new RuntimeException("No file provider authority specified in manifest");
   }

   @Override
   public void onStatusReceived(ServiceTaskStatusEx status) {
      _taskStatus = status;
      if (_taskStatus != null && _taskStatus.state == State.FINISHED) {
         _taskExecutionServiceController.requestResult();
      }
   }

   @Override
   public void onResultReceived(ServiceTask<?> result) {
      CreateMrdBackupTask task = (CreateMrdBackupTask) result;
      try {
         _isPdfGenerated = task.getResult();
      } catch (UserFacingException e) {
         _oomDetected = true;
         _mbwManager.reportIgnoredException(e);
      }
      if (_isPdfGenerated) {
         enableSharing();
      }
   }
}