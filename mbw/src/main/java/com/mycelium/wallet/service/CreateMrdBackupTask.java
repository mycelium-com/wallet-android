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

package com.mycelium.wallet.service;

import android.content.Context;
import android.os.AsyncTask;

import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.pdf.ExportDistiller;
import com.mycelium.wallet.pdf.ExportDistiller.ExportEntry;
import com.mycelium.wallet.pdf.ExportDistiller.ExportProgressTracker;
import com.mycelium.wallet.pdf.ExportPdfParameters;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.squareup.otto.Bus;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CreateMrdBackupTask extends AsyncTask<Void, Void, Boolean> {
   private static class EntryToExport implements Serializable {
      private static final long serialVersionUID = 1L;
      private String base58PrivateKey;
      private String label;
      private final Map<AddressType, Address> addresses;
      private boolean isBch;

      private EntryToExport(Map<AddressType, Address> addresses, String base58PrivateKey, String label, boolean isBch) {
         this.base58PrivateKey = base58PrivateKey;
         this.label = label;
         this.addresses = addresses;
         this.isBch = isBch;
      }
   }

   private KdfParameters kdfParameters;
   private List<EntryToExport> active;
   private List<EntryToExport> archived;
   private NetworkParameters networkParameters;
   private String exportFilePath;
   private String stretchStatusMessage;
   private String encryptStatusMessage;
   private String pdfStatusMessage;
   private Double encryptionProgress;
   private ExportProgressTracker pdfProgress;
   private Context context;
   private Bus bus;

   public CreateMrdBackupTask(KdfParameters kdfParameters, Context context, WalletManager walletManager, KeyCipher cipher,
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
               Address address = a.getReceivingAddress().get();
               Map<AddressType, Address> addressMap = new HashMap<>();
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
      stretchStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_stretching);
      encryptStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_encrypting);
      pdfStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_creating);
      this.context = context;
   }

   @Override
   protected Boolean doInBackground(Void ... voids) {
      try {
         // Generate Encryption parameters by doing key stretching
         EncryptionParameters encryptionParameters = EncryptionParameters.generate(kdfParameters);
         publishProgress();
         // Encrypt
         encryptionProgress = 0D;
         double increment = 1D / (active.size() + archived.size());

         // Encrypt active
         List<ExportEntry> encryptedActiveKeys = new LinkedList<>();
         for (EntryToExport e : active) {
            encryptedActiveKeys.add(createExportEntry(e, encryptionParameters, networkParameters));
            encryptionProgress += increment;
            publishProgress();
         }
         // Encrypt archived
         List<ExportEntry> encryptedArchivedKeys = new LinkedList<>();
         for (EntryToExport e : archived) {
            encryptedArchivedKeys.add(createExportEntry(e, encryptionParameters, networkParameters));
            encryptionProgress += increment;
            publishProgress();
         }

         // Generate PDF document
         String exportFormatString = "Mycelium Backup 1.1";
         ExportPdfParameters exportParameters = new ExportPdfParameters(new Date().getTime(), exportFormatString,
               encryptedActiveKeys, encryptedArchivedKeys);
         pdfProgress = new ExportProgressTracker(exportParameters.getAllEntries());
         ExportDistiller.exportPrivateKeysToFile(context, exportParameters, pdfProgress, exportFilePath);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return false;
      } catch (OutOfMemoryError | IOException e) {
         throw new RuntimeException(e);
      }

      publishProgress();
      return true;
   }

   protected void terminate() {
      // Tell scrypt to stop
      kdfParameters.terminate();
   }

   private static ExportEntry createExportEntry(EntryToExport toExport, EncryptionParameters parameters,
                                                NetworkParameters network) {
      String encrypted = null;
      if (toExport.base58PrivateKey != null) {
         encrypted = MrdExport.V1.encryptPrivateKey(parameters, toExport.base58PrivateKey, network);
      }
      return new ExportEntry(toExport.addresses, encrypted, null, toExport.label, toExport.isBch);
   }

   @Override
   protected void onProgressUpdate(Void... voids) {
      super.onProgressUpdate(voids);
      if (pdfProgress != null) {
         bus.post(new ServiceTaskStatusEx(pdfStatusMessage, pdfProgress.getProgress(), ServiceTaskStatusEx.State.RUNNING));
      } else if (encryptionProgress != null) {
         bus.post(new ServiceTaskStatusEx(encryptStatusMessage, encryptionProgress, ServiceTaskStatusEx.State.RUNNING));
      } else {
         bus.post(new ServiceTaskStatusEx(stretchStatusMessage, kdfParameters.getProgress(), ServiceTaskStatusEx.State.RUNNING));
      }
   }

   @Override
   protected void onPostExecute(Boolean success) {
      bus.post(new BackupResult(success));
   }

   public static class BackupResult {
      public boolean success;

      public BackupResult(boolean success) {
         this.success = success;
      }
   }
}
