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

package com.mycelium.wallet.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;

import com.mrd.bitlib.crypto.MrdExport;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.UserFacingException;
import com.mycelium.wallet.pdf.ExportDistiller;
import com.mycelium.wallet.pdf.ExportDistiller.ExportEntry;
import com.mycelium.wallet.pdf.ExportDistiller.ExportProgressTracker;
import com.mycelium.wallet.pdf.ExportPdfParameters;

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
   private List<EntryToExport> _active;
   private List<EntryToExport> _archived;
   private NetworkParameters _network;
   private String _exportFilePath;
   private String _stretchStatusMessage;
   private String _encryptStatusMessage;
   private String _pdfStatusMessage;
   private Double _encryptionProgress;
   private ExportProgressTracker _pdfProgress;

   public CreateMrdBackupTask(KdfParameters kdfParameters, Context context, RecordManager recordManager,
         AddressBookManager addressBook, NetworkParameters network, String exportFilePath) {
      _kdfParameters = kdfParameters;
      _active = getListToExport(recordManager.getRecords(Tag.ACTIVE), addressBook, network);
      _archived = getListToExport(recordManager.getRecords(Tag.ARCHIVE), addressBook, network);
      _exportFilePath = exportFilePath;
      _network = network;
      _stretchStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_stretching);
      _encryptStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_encrypting);
      _pdfStatusMessage = context.getResources().getString(R.string.encrypted_pdf_backup_creating);
   }

   private static List<EntryToExport> getListToExport(List<Record> records, AddressBookManager addressBook,
         NetworkParameters network) {
      List<EntryToExport> result = new LinkedList<EntryToExport>();
      for (Record r : records) {
         String address = r.address.toString();
         if (r.hasPrivateKey()) {
            result.add(new EntryToExport(address, r.key.getBase58EncodedPrivateKey(network), addressBook
                  .getNameByAddress(address)));
         } else {
            result.add(new EntryToExport(address, null, addressBook.getNameByAddress(address)));
         }
      }
      return result;
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
         String exportFormatString = "Mycelium Backup 1.0";
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
         NetworkParameters network) {
      String encrypted = null;
      if (toExport.base58PrivateKey != null) {
         encrypted = MrdExport.V1.encrypt(parameters, toExport.base58PrivateKey, network);
      }
      return new ExportEntry(toExport.address, encrypted, toExport.label);
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
