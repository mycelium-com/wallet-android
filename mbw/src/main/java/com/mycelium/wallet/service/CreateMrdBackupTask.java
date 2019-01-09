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

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.crypto.PrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.R;
import com.mycelium.wallet.UserFacingException;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.colu.ColuAccount;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateMrdBackupTask extends ServiceTask<Boolean> {
   private static final long serialVersionUID = 1L;

   private static class EntryToExport implements Serializable {
      private static final long serialVersionUID = 1L;
      public String base58PrivateKey;
      public String label;
      private final WalletAccount.Type accountType;
      private final Map<AddressType, Address> addresses;

      public EntryToExport(Map<AddressType, Address> addresses, String base58PrivateKey, String label, WalletAccount.Type accountType) {
         this.base58PrivateKey = base58PrivateKey;
         this.label = label;
         this.accountType = accountType;
         this.addresses = addresses;
      }
   }

   private KdfParameters _kdfParameters;
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

      // Populate the active and archived entries to export
      _active = new LinkedList<>();
      _archived = new LinkedList<>();
      List<WalletAccount> accounts = new ArrayList<>();
      for (UUID id : walletManager.getUniqueIds()) {
         WalletAccount account = walletManager.getAccount(id);
         if (account.canSpend()) {
            accounts.add(account);
         }
      }
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

            String base58EncodedPrivateKey = null;
            if (a.canSpend()) {
               try {
                  base58EncodedPrivateKey = a.getPrivateKey(cipher).getBase58EncodedPrivateKey(network);
                  entry = new EntryToExport(a.getPublicKey().getAllSupportedAddresses(network),
                          base58EncodedPrivateKey, label, account.getType());

               } catch (KeyCipher.InvalidKeyCipher e) {
                  throw new RuntimeException(e);
               }
            } else {
               Address address = a.getReceivingAddress().get();
               Map<AddressType, Address> addressMap= new HashMap<>();
               addressMap.put(address.getType(), address);
               entry = new EntryToExport(addressMap, null, label, account.getType());
            }
         } else if (account instanceof ColuAccount) {
            ColuAccount a = (ColuAccount) account;
            String label = storage.getLabelByAccount(a.getId());
            String base58EncodedPrivateKey = null;
            if (a.canSpend()) {
               base58EncodedPrivateKey = a.getPrivateKey().getBase58EncodedPrivateKey(network);
               entry = new EntryToExport(a.getPrivateKey().getPublicKey().getAllSupportedAddresses(network),
                       base58EncodedPrivateKey, label, account.getType());
            }
         }

         if (entry != null) {
            if (account.isActive()) {
               _active.add(entry);
            } else {
               _archived.add(entry);
            }
            storage.setOtherAccountBackupState(account.getId(), MetadataStorage.BackupState.NOT_VERIFIED);
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

         // Encrypt active
         List<ExportEntry> encryptedActiveKeys = new LinkedList<>();
         for (EntryToExport e : _active) {
            encryptedActiveKeys.add(createExportEntry(e, encryptionParameters, _network, e.accountType));
            _encryptionProgress += increment;
         }
         // Encrypt archived
         List<ExportEntry> encryptedArchivedKeys = new LinkedList<>();
         for (EntryToExport e : _archived) {
            encryptedArchivedKeys.add(createExportEntry(e, encryptionParameters, _network, e.accountType));
            _encryptionProgress += increment;
         }

         // Generate PDF document
         String exportFormatString = "Mycelium Backup 1.1";
         ExportPdfParameters exportParameters = new ExportPdfParameters(new Date().getTime(), exportFormatString,
               encryptedActiveKeys, encryptedArchivedKeys);
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
                                                NetworkParameters network, WalletAccount.Type accountType) {
      String encrypted = null;
      if (toExport.base58PrivateKey != null) {
         encrypted = MrdExport.V1.encryptPrivateKey(parameters, toExport.base58PrivateKey, network);
      }
      return new ExportEntry(toExport.addresses, encrypted, null, toExport.label, accountType);
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
