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
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.R;
import com.mycelium.wallet.UserFacingException;
import com.mycelium.wallet.pdf.ExportDistiller;
import com.mycelium.wallet.pdf.ExportDistiller.ExportEntry;
import com.mycelium.wallet.pdf.ExportDistiller.ExportProgressTracker;
import com.mycelium.wallet.pdf.ExportPdfParameters;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CreateMrdBackupTask extends ServiceTask<Boolean> {
   private static final long serialVersionUID = 1L;

   private static class EntryToExport implements Serializable {
      private static final long serialVersionUID = 1L;

      public String address;
      public String base58PrivateKey;
      public String label;

      public EntryToExport(String address, String base58PrivateKey, String label) {
         this.address = address;
         this.base58PrivateKey = base58PrivateKey;
         this.label = label;
      }
   }

   private KdfParameters _kdfParameters;
   private Bip39.MasterSeed _masterSeed;
   private List<EntryToExport> _active;
   private List<EntryToExport> _archived;
   private NetworkParameters _network;
   private String _exportFilePath;
   private String _stretchStatusMessage;
   private String _encryptStatusMessage;
   private String _pdfStatusMessage;
   private Double _encryptionProgress;
   private ExportProgressTracker _pdfProgress;

   public CreateMrdBackupTask(KdfParameters kdfParameters, Context context, WalletManager walletManager, KeyCipher cipher,
                              MetadataStorage storage, NetworkParameters network, String exportFilePath) {
      _kdfParameters = kdfParameters;

      // Fetch the master seed if we have it
      if (walletManager.hasBip32MasterSeed()) {
         try {
            _masterSeed = walletManager.getMasterSeed(cipher);
         } catch (KeyCipher.InvalidKeyCipher e) {
            throw new RuntimeException(e);
         }
      }

      // Populate the active and archived entries to export
      _active = new LinkedList<EntryToExport>();
      _archived = new LinkedList<EntryToExport>();
      for (UUID id : walletManager.getAccountIds()) {
         WalletAccount account = walletManager.getAccount(id);
         if (!(account instanceof SingleAddressAccount)) {
            continue;
         }
         EntryToExport entry;
         SingleAddressAccount a = (SingleAddressAccount) account;
         if (a.canSpend()) {
            String base58EncodedPrivateKey;
            try {
               base58EncodedPrivateKey = a.getPrivateKey(cipher).getBase58EncodedPrivateKey(network);
            } catch (KeyCipher.InvalidKeyCipher e) {
               throw new RuntimeException(e);
            }
            Address address = a.getAddress();
            String label = storage.getLabelByAccount(a.getId());
            entry = new EntryToExport(address.toString(), base58EncodedPrivateKey, label);
         } else {
            Address address = a.getAddress();
            String label = storage.getLabelByAccount(a.getId());
            entry = new EntryToExport(address.toString(), null, label);
         }
         if (a.isActive()) {
            _active.add(entry);
         } else {
            _archived.add(entry);
         }
      }

      _exportFilePath = exportFilePath;
      _network = network;
      _stretchStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_stretching);
      _encryptStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_encrypting);
      _pdfStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_creating);
   }

   @Override
   protected Boolean doTask(Context context) throws UserFacingException {
      try {
         // Generate Encryption parameters by doing key stretching
         EncryptionParameters encryptionParameters;
         try {
            encryptionParameters = EncryptionParameters.generate(_kdfParameters);
         } catch (InterruptedException e) {
            return false;
         }

         // Encrypt
         _encryptionProgress = 0D;
         double increment = 1D / (_active.size() + _archived.size());

         // Encrypt Master seed if present
         Optional<ExportEntry> encryptedMasterSeed;
         if (_masterSeed == null) {
            encryptedMasterSeed = Optional.absent();
         } else {
            String e = MrdExport.V1.encryptMasterSeed(encryptionParameters, _masterSeed, _network);
             encryptedMasterSeed = Optional.of(new ExportEntry(null, null, e, null));
         }

         // Encrypt active
         List<ExportEntry> encryptedActiveKeys = new LinkedList<ExportEntry>();
         for (EntryToExport e : _active) {
            encryptedActiveKeys.add(createExportEntry(e, encryptionParameters, _network));
            _encryptionProgress += increment;
         }
         // Encrypt archived
         List<ExportEntry> encryptedArchivedKeys = new LinkedList<ExportEntry>();
         for (EntryToExport e : _archived) {
            encryptedArchivedKeys.add(createExportEntry(e, encryptionParameters, _network));
            _encryptionProgress += increment;
         }

         // Generate PDF document
         String exportFormatString = "Mycelium Backup 1.1";
         ExportPdfParameters exportParameters = new ExportPdfParameters(new Date().getTime(), exportFormatString,
               encryptedMasterSeed, encryptedActiveKeys, encryptedArchivedKeys);
         _pdfProgress = new ExportProgressTracker(exportParameters.getAllEntries());
         ExportDistiller.exportPrivateKeysToFile(context, exportParameters, _pdfProgress, _exportFilePath);
      } catch (OutOfMemoryError e) {
         throw new UserFacingException(e);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }

      return true;
   }

   @Override
   protected void terminate() {
      // Tell scrypt to stop
      _kdfParameters.terminate();
   }

   private static ExportEntry createExportEntry(EntryToExport toExport, EncryptionParameters parameters,
                                                NetworkParameters network) {
      String encrypted = null;
      if (toExport.base58PrivateKey != null) {
         encrypted = MrdExport.V1.encryptPrivateKey(parameters, toExport.base58PrivateKey, network);
      }
      return new ExportEntry(toExport.address, encrypted, null, toExport.label);
   }

   @Override
   protected ServiceTaskStatus getStatus() {
      if (_pdfProgress != null) {
         return new ServiceTaskStatus(_pdfStatusMessage, _pdfProgress.getProgress());
      } else if (_encryptionProgress != null) {
         return new ServiceTaskStatus(_encryptStatusMessage, _encryptionProgress);
      } else {
         return new ServiceTaskStatus(_stretchStatusMessage, _kdfParameters.getProgress());
      }
   }

}
