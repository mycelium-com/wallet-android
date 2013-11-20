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

import static android.text.format.DateFormat.getDateFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.AndroidRandomSource;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.BackgroundKeyStretcher.PostRunner;
import com.mycelium.wallet.pdf.ExportDestiller;
import com.mycelium.wallet.pdf.ExportDestiller.ExportEntry;
import com.mycelium.wallet.pdf.ExportDestiller.ExportProgressTracker;

public class BackupToPdfActivity extends Activity {

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, BackupToPdfActivity.class);
      currentActivity.startActivity(intent);
   }

   private static final String FILE_NAME_PREFIX = "mycelium-backup";

   private MbwManager _mbwManager;
   private long _backupTime;
   private String _fileName;
   private String _password;
   private List<ExportEntry> _encryptedActiveKeys;
   private List<ExportEntry> _encryptedArchivedKeys;
   private MrdExport.V1.EncryptionParameters _exportParameters;
   private BackgroundKeyStretcher _stretcher;
   private AsyncTask<Void, Void, Void> _encryptionTask;
   private AsyncTask<Void, Void, String> _pdfTask;
   private boolean _isPdfGenerated;
   private Double _encryptProgress;
   private ExportProgressTracker _pdfProgressTracker;
   private ProgressUpdater _progressUpdater;

   /** Called when the activity is first created. */
   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.export_to_pdf_activity);
      Utils.preventScreeshots(this);

      _mbwManager = MbwManager.getInstance(this.getApplication());

      // Load saved state
      if (savedInstanceState != null) {
         _backupTime = savedInstanceState.getLong("backupTime", 0);
         _password = savedInstanceState.getString("password");
         _exportParameters = (EncryptionParameters) savedInstanceState.getSerializable("exportParameters");
         _isPdfGenerated = savedInstanceState.getBoolean("isPdfGenerated");
         _encryptedActiveKeys = (List<ExportEntry>) savedInstanceState.getSerializable("encryptedActiveKeys");
         _encryptedArchivedKeys = (List<ExportEntry>) savedInstanceState.getSerializable("encryptedArchivedKeys");
      }

      if (_backupTime == 0) {
         _backupTime = new Date().getTime();
      }

      if (_password == null) {
         _password = MrdExport.V1.generatePassword(new AndroidRandomSource()).toUpperCase(Locale.US);
      }

      _fileName = getExportFileName(_backupTime);
      _stretcher = new BackgroundKeyStretcher();

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

      _progressUpdater = new ProgressUpdater();
   }

   @Override
   protected void onSaveInstanceState(@SuppressWarnings("NullableProblems") Bundle outState) {
      outState.putLong("backupTime", _backupTime);
      outState.putString("password", _password);
      outState.putSerializable("exportParameters", _exportParameters);
      outState.putBoolean("isPdfGenerated", _isPdfGenerated);
      outState.putSerializable("encryptedActiveKeys", (Serializable) _encryptedActiveKeys);
      outState.putSerializable("encryptedArchivedKeys", (Serializable) _encryptedArchivedKeys);
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
      if (_exportParameters == null) {
         startKeyStretching();
      } else if (_encryptedActiveKeys == null || _encryptedArchivedKeys == null) {
         startEncryptionTask();
      } else if (!_isPdfGenerated) {
         startPdfGeneration();
      } else {
         enableSharing();
      }

      _progressUpdater.start();
      super.onResume();
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
         ExportProgressTracker pdfTracker = _pdfProgressTracker;
         Double encryptProgress = _encryptProgress;

         if (tracker != null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (tracker.getProgress() * 100) + "%");
         } else if (encryptProgress != null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (encryptProgress * 100) + "%");
         } else if (pdfTracker != null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (pdfTracker.getProgress() * 100) + "%");
         } else {
            ((TextView) findViewById(R.id.tvProgress)).setText("");
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
      _stretcher.terminate();
      if (_encryptionTask != null && _encryptionTask.getStatus() == Status.RUNNING) {
         _encryptionTask.cancel(true);
         _encryptionTask = null;
      }
      if (_pdfTask != null && _pdfTask.getStatus() == Status.RUNNING) {
         _pdfTask.cancel(true);
         _pdfTask = null;
      }
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
      return name.replace(':', '.').replace(' ', '-').replace('\\', '-').replace('/', '-').replace('*', '-')
            .replace('?', '-').replace('"', '-').replace('\'', '-').replace('<', '-').replace('>', '-')
            .replace('|', '-');
   }

   private String getFullExportFilePath() {
      return _fileName;
   }

   private void startKeyStretching() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.encrypted_pdf_backup_stretching);
      KdfParameters kdfParameters = KdfParameters.createNewFromPassphrase(_password, new AndroidRandomSource());
      _stretcher.start(kdfParameters, new PostRunner() {

         @Override
         public void onPostExecute(boolean error, EncryptionParameters parameters) {
            if (error) {
               ((TextView) findViewById(R.id.tvStatus)).setText(R.string.out_of_memory_error);
            } else {
               _exportParameters = parameters;
               startEncryptionTask();
            }
         }
      });
   }

   private void startEncryptionTask() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.encrypted_pdf_backup_encrypting);
      _encryptionTask = new EncryptionTask(_exportParameters).execute();
   }

   private class EncryptionTask extends AsyncTask<Void, Void, Void> {

      private MrdExport.V1.EncryptionParameters _parameters;
      private List<ExportEntry> _active;
      private List<ExportEntry> _archived;

      public EncryptionTask(MrdExport.V1.EncryptionParameters p) {
         _parameters = p;
      }

      @Override
      protected Void doInBackground(Void... params) {

         AddressBookManager addressBook = _mbwManager.getAddressBookManager();
         List<Record> activeRecords = _mbwManager.getRecordManager().getRecords(Tag.ACTIVE);
         List<Record> archivedRecords = _mbwManager.getRecordManager().getRecords(Tag.ARCHIVE);
         NetworkParameters network = _mbwManager.getNetwork();
         _encryptProgress = 0D;
         double increment = 1D / (activeRecords.size() + archivedRecords.size());
         // Encrypt all active records
         _active = new LinkedList<ExportEntry>();
         for (Record r : activeRecords) {
            _active.add(createExportEntry(r, _parameters, addressBook, network));
            _encryptProgress += increment;
         }

         // Encrypt all archived records
         _archived = new LinkedList<ExportEntry>();
         for (Record r : archivedRecords) {
            _archived.add(createExportEntry(r, _parameters, addressBook, network));
            _encryptProgress += increment;
         }
         _encryptProgress = null;
         return null;
      }

      private ExportEntry createExportEntry(Record r, MrdExport.V1.EncryptionParameters parameters,
            AddressBookManager addressBook, NetworkParameters network) {
         String encrypted = null;
         if (r.hasPrivateKey()) {
            encrypted = MrdExport.V1.encrypt(parameters, r.key.getBase58EncodedPrivateKey(network), network);
         }
         String address = r.address.toString();
         String label = addressBook.getNameByAddress(address);
         return new ExportEntry(address, encrypted, label);
      }

      @Override
      protected void onPostExecute(Void v) {
         _encryptionTask = null;
         _encryptedActiveKeys = _active;
         _encryptedArchivedKeys = _archived;
         startPdfGeneration();
         super.onPostExecute(v);
      }

   }

   private void startPdfGeneration() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.encrypted_pdf_backup_creating);
      _pdfTask = new GeneratePdfTask().execute();
   }

   private class GeneratePdfTask extends AsyncTask<Void, Void, String> {

      private String _pdfString;

      @SuppressLint("WorldReadableFiles")
      @Override
      protected String doInBackground(Void... params) {

         String exportFormatString = "Mycelium Backup 1.0";

         // Generate PDF document
         _pdfProgressTracker = ExportDestiller
               .createExportProgressTracker(_encryptedActiveKeys, _encryptedArchivedKeys);
         _pdfString = ExportDestiller.exportPrivateKeys(BackupToPdfActivity.this, exportFormatString, _backupTime,
               _encryptedActiveKeys, _encryptedArchivedKeys, _pdfProgressTracker);
         _pdfProgressTracker = null;

         // Write document to file
         try {

            FileOutputStream stream;
            stream = getOutStream();
            stream.write(_pdfString.getBytes("UTF-8"));
            stream.close();
         } catch (IOException e) {
            return "An error occurred while writing backup document to file: " + getFullExportFilePath();
         }

         return null;
      }

      @Override
      protected void onPostExecute(String error) {
         if (error != null) {
            ((TextView) findViewById(R.id.tvStatus)).setText(error);
            return;
         }
         _pdfTask = null;
         _isPdfGenerated = true;
         enableSharing();
      }

   }

   private FileOutputStream getOutStream() throws FileNotFoundException {
      return openFileOutput(getFullExportFilePath(), MODE_PRIVATE);
   }

   private void enableSharing() {
      findViewById(R.id.btSharePdf).setEnabled(true);
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
      startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_with)));
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
         @SuppressWarnings("ConstantConditions")
         String packageName = resolveInfo.activityInfo.packageName;
         grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      }
   }

   private Uri getUri() {
      String authority = getFileProviderAuthority();
      return FileProvider.getUriForFile(this, authority, getFileStreamPath(getFullExportFilePath()));
   }


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
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      // revoke permissions
      revokeUriPermission(getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION);

      File exportedFile = getFileStreamPath(getFullExportFilePath());
      boolean deleted = exportedFile.delete();
      Preconditions.checkArgument(deleted);
      super.onActivityResult(requestCode, resultCode, data);
   }

   public boolean hasExternalStorage() {
      return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
   }

}