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
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import android.view.WindowManager;
import android.widget.TextView;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.*;
import com.mycelium.wallet.pdf.ExportDistiller;
import com.mycelium.wallet.pdf.ExportPdfParameters;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

//todo HD: export master seed without address/xpub extra data.
//todo HD: later: be compatible with a common format
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.text.format.DateFormat.getDateFormat;

public class BackupToPdfActivity extends AppCompatActivity {
   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, BackupToPdfActivity.class);
      currentActivity.startActivity(intent);
   }

   private static final String FILE_NAME_PREFIX = "mycelium-backup";

   private static final int SHARE_REQUEST_CODE = 1;

   private MbwManager mbwManager;
   private long backupTime;
   private String fileName;
   private String password;
   private boolean isPdfGenerated;
   private CreateMrdBackupTask task;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.export_to_pdf_activity);
      Utils.preventScreenshots(this);

      mbwManager = MbwManager.getInstance(getApplication());

      // Load saved state
      if (savedInstanceState != null) {
         backupTime = savedInstanceState.getLong("backupTime", 0);
         password = savedInstanceState.getString("password");
         isPdfGenerated = savedInstanceState.getBoolean("isPdfGenerated");
      }

      if (backupTime == 0) {
         backupTime = new Date().getTime();
      }

      if (password == null) {
         password = MrdExport.V1.generatePassword(new AndroidRandomSource()).toUpperCase(Locale.US);
      }

      fileName = getExportFileName(backupTime);

      // Populate Password
      ((TextView) findViewById(R.id.tvPassword)).setText(splitPassword(password));

      // Populate Checksum
      char checksumChar = MrdExport.V1.calculatePasswordChecksum(password);
      String checksumString = ("  " + checksumChar).toUpperCase(Locale.US);
      ((TextView) findViewById(R.id.tvChecksum)).setText(checksumString);

      findViewById(R.id.btSharePdf).setOnClickListener(arg0 -> sharePdf());

      findViewById(R.id.btVerify).setOnClickListener(view -> VerifyBackupActivity.callMe(this));
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putLong("backupTime", backupTime);
      outState.putString("password", password);
      outState.putBoolean("isPdfGenerated", isPdfGenerated);
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
      MbwManager.getEventBus().register(this);
      if (!isPdfGenerated) {
         startTask();
      } else {
         enableSharing();
      }
      super.onResume();
   }

   @Override
   protected void onPause() {
      MbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   protected void onDestroy() {
      if (task != null && !task.isCancelled()) {
         task.cancel(true);
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
      return fileName;
   }

   private void startTask() {
      findViewById(R.id.btSharePdf).setEnabled(false);
      findViewById(R.id.btVerify).setEnabled(false);
      KdfParameters kdfParameters = KdfParameters.createNewFromPassphrase(password, new AndroidRandomSource(),
            mbwManager.getDeviceScryptParameters());
      task = new CreateMrdBackupTask(kdfParameters, getApplicationContext(),
            mbwManager.getWalletManager(false), AesKeyCipher.defaultKeyCipher(), mbwManager.getMetadataStorage(),
            mbwManager.getNetwork(), getFullExportFilePath());
      task.execute();
   }

   private void enableSharing() {
      findViewById(R.id.btSharePdf).setEnabled(true);
      findViewById(R.id.btVerify).setEnabled(true);
      ((TextView) findViewById(R.id.tvProgress)).setText("");
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
      startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.share_with)), SHARE_REQUEST_CODE);
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
            if (info.name.equals("androidx.core.content.FileProvider")) {
               return info.authority;
            }
         }
      } catch (NameNotFoundException e) {
         throw new RuntimeException(e);
      }
      throw new RuntimeException("No file provider authority specified in manifest");
   }

   @Subscribe
   public void onResultReceived(BackupResult result) {
      isPdfGenerated = result.success;
      if (isPdfGenerated) {
         enableSharing();
      }
   }

   @Subscribe
   public void onProgressReceived(BackupProgress progress) {
      if (isPdfGenerated) {
         return;
      }
      ((TextView) findViewById(R.id.tvProgress)).setText(progress.message);
      ((TextView) findViewById(R.id.tvStatus)).setText("" + (int) (progress.progress * 100) + "%");
   }

   private static class CreateMrdBackupTask extends AsyncTask<Void, Void, Boolean> {
      private KdfParameters kdfParameters;
      private List<EntryToExport> active;
      private List<EntryToExport> archived;
      private NetworkParameters networkParameters;
      private String exportFilePath;
      private Double encryptionProgress;
      private ExportDistiller.ExportProgressTracker pdfProgress;
      private Context context;
      private Bus bus;

      private CreateMrdBackupTask(KdfParameters kdfParameters, Context context, WalletManager walletManager, KeyCipher cipher,
                                 MetadataStorage storage, NetworkParameters network, String exportFilePath) {
         this.kdfParameters = kdfParameters;
         this.bus = MbwManager.getEventBus();

         // Populate the active and archived entries to export
         active = new LinkedList<>();
         archived = new LinkedList<>();
         List<WalletAccount<?>> accounts = walletManager.getSpendingAccounts();
         accounts = Utils.sortAccounts(accounts, storage);
         EntryToExport entry;
         for (WalletAccount account : accounts) {
            //TODO: add check whether coluaccount is in hd or singleaddress mode
            entry = null;
            if (account instanceof SingleAddressAccount) {
               if (!account.isVisible()) {
                  continue;
               }
               SingleAddressAccount a = (SingleAddressAccount) account;
               String label = storage.getLabelByAccount(a.getId());

               String base58EncodedPrivateKey;
               if (a.canSpend()) {
                  try {
                     base58EncodedPrivateKey = a.getPrivateKey(cipher).getBase58EncodedPrivateKey(network);
                     entry = new EntryToExport(a.getPublicKey().getAllSupportedAddresses(network),
                             base58EncodedPrivateKey, label, account instanceof SingleAddressBCHAccount);

                  } catch (KeyCipher.InvalidKeyCipher e) {
                     throw new RuntimeException(e);
                  }
               } else {
                  BitcoinAddress address = a.getReceivingAddress().get();
                  Map<AddressType, BitcoinAddress> addressMap = new HashMap<>();
                  addressMap.put(address.getType(), address);
                  entry = new EntryToExport(addressMap, null, label, account instanceof SingleAddressBCHAccount);
               }
            } else if (account instanceof ColuAccount && account.canSpend()) {
               ColuAccount a = (ColuAccount) account;
               String label = storage.getLabelByAccount(a.getId());
               String base58EncodedPrivateKey = a.getPrivateKey().getBase58EncodedPrivateKey(network);
               entry = new EntryToExport(a.getPrivateKey().getPublicKey().getAllSupportedAddresses(network),
                       base58EncodedPrivateKey, label, false);
            }

            if (entry != null) {
               if (account.isActive()) {
                  active.add(entry);
               } else {
                  archived.add(entry);
               }
               storage.setOtherAccountBackupState(account.getId(), MetadataStorage.BackupState.NOT_VERIFIED);
            }
         }

         this.exportFilePath = exportFilePath;
         networkParameters = network;
         this.context = context;
      }

      @Override
      protected Boolean doInBackground(Void ... voids) {
         try {
            // Generate Encryption parameters by doing key stretching
            MrdExport.V1.EncryptionParameters encryptionParameters = MrdExport.V1.EncryptionParameters.generate(kdfParameters);
            publishProgress();
            // Encrypt
            encryptionProgress = 0D;
            double increment = 1D / (active.size() + archived.size());

            // Encrypt active
            List<ExportDistiller.ExportEntry> encryptedActiveKeys = new LinkedList<>();
            for (EntryToExport e : active) {
               encryptedActiveKeys.add(createExportEntry(e, encryptionParameters, networkParameters));
               encryptionProgress += increment;
               publishProgress();
            }
            // Encrypt archived
            List<ExportDistiller.ExportEntry> encryptedArchivedKeys = new LinkedList<>();
            for (EntryToExport e : archived) {
               encryptedArchivedKeys.add(createExportEntry(e, encryptionParameters, networkParameters));
               encryptionProgress += increment;
               publishProgress();
            }

            // Generate PDF document
            String exportFormatString = "Mycelium Backup 1.1";
            ExportPdfParameters exportParameters = new ExportPdfParameters(new Date().getTime(), exportFormatString,
                    encryptedActiveKeys, encryptedArchivedKeys);
            pdfProgress = new ExportDistiller.ExportProgressTracker(exportParameters.getAllEntries());
            ExportDistiller.exportPrivateKeysToFile(context, exportParameters, pdfProgress, exportFilePath);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
         } catch (OutOfMemoryError | IOException e) {
            return false;
         }
         return true;
      }

      private static ExportDistiller.ExportEntry createExportEntry(EntryToExport toExport, MrdExport.V1.EncryptionParameters parameters,
                                                                   NetworkParameters network) {
         String encrypted = null;
         if (toExport.base58PrivateKey != null) {
            encrypted = MrdExport.V1.encryptPrivateKey(parameters, toExport.base58PrivateKey, network);
         }
         return new ExportDistiller.ExportEntry(toExport.addresses, encrypted, null, toExport.label, toExport.isBch);
      }

      @Override
      protected void onProgressUpdate(Void... voids) {
         super.onProgressUpdate(voids);
         if (pdfProgress != null) {
            bus.post(new BackupProgress(R.string.encrypted_pdf_backup_creating, pdfProgress.getProgress()));
         } else if (encryptionProgress != null) {
            bus.post(new BackupProgress(R.string.encrypted_pdf_backup_encrypting, encryptionProgress));
         } else {
            bus.post(new BackupProgress(R.string.encrypted_pdf_backup_stretching));
         }
      }

      @Override
      protected void onPostExecute(Boolean success) {
         bus.post(new BackupResult(success));
      }
   }

   private static class EntryToExport implements Serializable {
      private static final long serialVersionUID = 1L;
      private String base58PrivateKey;
      private String label;
      private final Map<AddressType, BitcoinAddress> addresses;
      private boolean isBch;

      private EntryToExport(Map<AddressType, BitcoinAddress> addresses, String base58PrivateKey, String label, boolean isBch) {
         this.base58PrivateKey = base58PrivateKey;
         this.label = label;
         this.addresses = addresses;
         this.isBch = isBch;
      }
   }

   private static class BackupResult {
      boolean success;

      BackupResult(boolean success) {
         this.success = success;
      }
   }

   private static class BackupProgress {
      @StringRes
      Integer message;
      double progress;

      BackupProgress(Integer message, double progress) {
         this.message = message;
         this.progress = progress;
      }

      BackupProgress(Integer message) {
         this.message = message;
      }
   }
}
