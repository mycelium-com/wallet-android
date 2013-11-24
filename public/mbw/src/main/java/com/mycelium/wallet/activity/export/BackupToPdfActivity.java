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
import android.os.Build;
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
import com.mycelium.wallet.pdf.ExportDestiller;
import com.mycelium.wallet.pdf.ExportDestiller.ExportEntry;
import com.mycelium.wallet.service.ExportService;
import com.mycelium.wallet.service.ExportServiceController;
import com.mycelium.wallet.service.ExportServiceController.ExportServiceCallback;
import com.mycelium.wallet.service.KeyStretcherService;
import com.mycelium.wallet.service.KeyStretcherServiceController;
import com.mycelium.wallet.service.KeyStretcherServiceController.KeyStretcherServiceCallback;

public class BackupToPdfActivity extends Activity implements KeyStretcherServiceCallback, ExportServiceCallback {

   private static final String MYCELIUM_EXPORT_FOLDER = "mycelium";

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
   private AsyncTask<Void, Void, Void> _encryptionTask;
   // private AsyncTask<Void, Void, String> _pdfTask;
   private boolean _isPdfGenerated;
   private Double _encryptProgress;
   private ProgressUpdater _progressUpdater;
   private KeyStretcherServiceController _keyStretcherServiceController;
   private ExportServiceController _exportServiceController;
   private KeyStretcherService.Status _stretchingStatus;
   private ExportService.Status _exportStatus;

   /** Called when the activity is first created. */
   @SuppressWarnings("unchecked")
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
      _keyStretcherServiceController = new KeyStretcherServiceController();

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
         Double encryptProgress = _encryptProgress;

         if (_exportParameters == null) {
            _keyStretcherServiceController.requestStatus();
         }

         if (_exportServiceController != null) {
            _exportServiceController.requestStatus();
         }

         if (_stretchingStatus != null && _exportParameters == null) {
            if (_stretchingStatus.error) {
               ((TextView) findViewById(R.id.tvStatus)).setText(getResources().getString(R.string.out_of_memory_error));
               ((TextView) findViewById(R.id.tvProgress)).setText("");
            } else {
               ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (_stretchingStatus.progress * 100) + "%");
            }
         } else if (encryptProgress != null) {
            ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (encryptProgress * 100) + "%");
         } else if (_exportStatus != null && !_isPdfGenerated) {
            if (_exportStatus.errorMessage != null) {
               ((TextView) findViewById(R.id.tvStatus)).setText(_exportStatus.errorMessage);
               ((TextView) findViewById(R.id.tvProgress)).setText("");
            } else {
               ((TextView) findViewById(R.id.tvProgress)).setText("" + (int) (_exportStatus.progress * 100) + "%");
            }
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
      _keyStretcherServiceController.terminate();
      _keyStretcherServiceController.unbind(this);
      if (_encryptionTask != null && _encryptionTask.getStatus() == Status.RUNNING) {
         _encryptionTask.cancel(true);
         _encryptionTask = null;
      }
      if (_exportServiceController != null) {
         _exportServiceController.terminate();
         _exportServiceController.unbind(this);
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
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
         return _fileName;
      } else {
         File externalStorageDir = Environment.getExternalStorageDirectory();
         String directory = externalStorageDir.getAbsolutePath() + File.separatorChar + MYCELIUM_EXPORT_FOLDER;
         File dirFile = new File(directory);
         if (!dirFile.exists()) {
            boolean wasCreated = dirFile.mkdirs();
            Preconditions.checkArgument(wasCreated);
         } else {
            Preconditions.checkArgument(dirFile.isDirectory());
         }
         return directory + File.separatorChar + _fileName;
      }
   }

   private void startKeyStretching() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      ((TextView) findViewById(R.id.tvStatus)).setText(R.string.encrypted_pdf_backup_stretching);
      KdfParameters kdfParameters = KdfParameters.createNewFromPassphrase(_password, new AndroidRandomSource());
      _keyStretcherServiceController.bind(this, this);
      _keyStretcherServiceController.start(kdfParameters);
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

      _exportServiceController = new ExportServiceController();

      String exportFormatString = "Mycelium Backup 1.0";

      // Generate PDF document
      ExportDestiller.ExportPdfParameters p = new ExportDestiller.ExportPdfParameters(_backupTime, exportFormatString,
            _encryptedActiveKeys, _encryptedArchivedKeys);
      _exportServiceController.bind(this, this);
      _exportServiceController.start(p, getFullExportFilePath());
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
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
         String authority = getFileProviderAuthority();
         return FileProvider.getUriForFile(this, authority, getFileStreamPath(getFullExportFilePath()));
      } else {
         return Uri.fromFile(new File(getFullExportFilePath()));
      }
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

   @Override
   public void onStatusReceived(KeyStretcherService.Status status) {
      _stretchingStatus = status;
      if (_stretchingStatus.hasResult) {
         _keyStretcherServiceController.requestResult();
      }
   }

   @Override
   public void onResultReceived(EncryptionParameters parameters) {
      if (_exportParameters == null && parameters != null) {
         _exportParameters = parameters;
         startEncryptionTask();
      }
   }

   @Override
   public void onExportStatusReceived(ExportService.Status status) {
      if (_exportServiceController == null) {
         return;
      }
      _exportStatus = status;
      if (_exportStatus.isComplete) {
         if (_exportStatus.errorMessage == null) {
            _exportServiceController.requestResult();
         } else {
            _exportServiceController.terminate();
            _exportServiceController.unbind(this);
            _exportServiceController = null;
         }
      }
   }

   @Override
   public void onExportResultReceived(String result) {

      // Write document to file
      try {

         FileOutputStream stream;
         stream = getOutStream(this, getFullExportFilePath());
         stream.write(result.getBytes("UTF-8"));
         stream.close();
      } catch (IOException e) {
         ((TextView) findViewById(R.id.tvStatus)).setText("An error occurred while writing backup document to file: "
               + getFullExportFilePath());
         ((TextView) findViewById(R.id.tvProgress)).setText("");
         _exportStatus = null;
      }
      _isPdfGenerated = true;
      _exportServiceController.terminate();
      _exportServiceController.unbind(this);
      _exportServiceController = null;
      enableSharing();
   }

   private static FileOutputStream getOutStream(Context context, String filePath) throws FileNotFoundException {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
         return context.openFileOutput(filePath, Context.MODE_PRIVATE);
      } else {
         return new FileOutputStream(filePath);
      }
   }

}