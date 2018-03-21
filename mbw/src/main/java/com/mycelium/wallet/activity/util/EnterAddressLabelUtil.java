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

package com.mycelium.wallet.activity.util;

import android.content.Context;

import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.EnterTextDialog;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.squareup.otto.Bus;

import java.util.UUID;

public class EnterAddressLabelUtil {
   public interface AddressLabelChangedHandler {
      void OnAddressLabelChanged(Address address, String label);
   }

   public interface TransactionLabelChangedHandler {
      void OnTransactionLabelChanged(Sha256Hash txid, String label);
   }

   public static void enterAddressLabel(Context context, MetadataStorage storage, Address address,
                                        String defaultName, AddressLabelChangedHandler changeHandler) {
      String hintText = context.getResources().getString(R.string.name);
      String currentName = storage.getLabelByAddress(address);
      int title_id;
      if (currentName.length() == 0) {
         title_id = R.string.enter_address_label_title;
         currentName = defaultName;
      } else {
         title_id = R.string.edit_address_label_title;
      }
      currentName = currentName.length() == 0 ? defaultName : currentName;
      String invalidOkToastMessage = context.getResources().getString(R.string.address_label_not_unique);
      EnterTextDialog.show(context, title_id, hintText, currentName, true, new EnterAddressLabelHandler(storage,
              address, invalidOkToastMessage, changeHandler));
   }

   private static class EnterAddressLabelHandler extends EnterTextDialog.EnterTextHandler {
      private MetadataStorage _storage;
      private Address _address;
      private String _invalidOkToastMessage;
      private AddressLabelChangedHandler _changeHandler;

      EnterAddressLabelHandler(MetadataStorage storage, Address address, String invalidOkToastMessage,
                               AddressLabelChangedHandler changeHandler) {
         _storage = storage;
         _address = address;
         _invalidOkToastMessage = invalidOkToastMessage;
         _changeHandler = changeHandler;
      }

      @Override
      public boolean validateTextOnChange(String newText, String oldText) {
         return true;
      }

      @Override
      public boolean validateTextOnOk(String newText, String oldText) {
         // Make sure that no address exists with that name, or that we are
         // updating the existing entry with the same name. It is OK for the
         // name to be empty, in which case it will get deleted
         Optional<Address> address = _storage.getAddressByLabel(newText);
         return !address.isPresent() || address.get().equals(_address);
      }

      @Override
      public boolean getVibrateOnInvalidOk(String newText, String oldText) {
         return true;
      }

      @Override
      public String getToastTextOnInvalidOk(String newText, String oldText) {
         return _invalidOkToastMessage;
      }

      @Override
      public void onNameEntered(String newText, String oldText) {
         // No address exists with that name, or we are updating the
         // existing entry with the same name. If the name is blank the
         // entry will get deleted
         _storage.storeAddressLabel(_address, newText);
         if (_changeHandler != null) {
            _changeHandler.OnAddressLabelChanged(_address, newText);
         }
      }
   }

   public static void enterAccountLabel(final Context context, UUID account, String defaultName, MetadataStorage storage) {
      String hintText = context.getResources().getString(R.string.name);
      String currentName = storage.getLabelByAccount(account);
      int title_id;
      if (currentName.length() == 0) {
         title_id = R.string.enter_account_label_title;
         currentName = defaultName;
      } else {
         title_id = R.string.edit_account_label_title;
      }
      String invalidOkToastMessage = context.getResources().getString(R.string.account_label_not_unique);
      Bus bus = MbwManager.getInstance(context).getEventBus();
      EnterAccountLabelHandler handler = new EnterAccountLabelHandler(account, invalidOkToastMessage, storage, bus) {
         @Override
         public void onDismiss() {
            super.onDismiss();
            MbwManager.getInstance(context).importLabelsToBch(MbwManager.getInstance(context).getWalletManager(false));
         }
      };
      EnterTextDialog.show(context, title_id, hintText, currentName, true, handler);
   }

   private static class EnterAccountLabelHandler extends EnterTextDialog.EnterTextHandler {
      private UUID account;
      private String invalidOkToastMessage;
      private MetadataStorage storage;
      private Bus bus;

      EnterAccountLabelHandler(UUID account, String invalidOkToastMessage, MetadataStorage storage, Bus bus) {
         this.account = account;
         this.invalidOkToastMessage = invalidOkToastMessage;
         this.storage = storage;
         this.bus = bus;
      }

      @Override
      public boolean validateTextOnOk(String newText, String oldText) {
         Optional<UUID> existing = storage.getAccountByLabel(newText);
         return !existing.isPresent() || existing.get().equals(account);
      }

      @Override
      public boolean getVibrateOnInvalidOk(String newText, String oldText) {
         return true;
      }

      @Override
      public String getToastTextOnInvalidOk(String newText, String oldText) {
         return invalidOkToastMessage;
      }

      @Override
      public void onNameEntered(String newText, String oldText) {
         storage.storeAccountLabel(account, newText);
         bus.post(new AccountChanged(account));
      }
   }

   public static void enterTransactionLabel(Context context, Sha256Hash txid, MetadataStorage storage, TransactionLabelChangedHandler changeHandler) {
      String hintText = context.getResources().getString(R.string.enter_transaction_label_title);
      String currentName = storage.getLabelByTransaction(txid);
      int title_id;
      title_id = R.string.enter_transaction_label_title;

      EnterTransactionLabelHandler handler = new EnterTransactionLabelHandler(txid, storage, changeHandler);
      EnterTextDialog.show(context, title_id, hintText, currentName, true, handler);
   }

   private static class EnterTransactionLabelHandler extends EnterTextDialog.EnterTextHandler {
      private Sha256Hash txid;
      private MetadataStorage storage;
      private TransactionLabelChangedHandler changeHandler;

      EnterTransactionLabelHandler(Sha256Hash txid, MetadataStorage storage, TransactionLabelChangedHandler changeHandler) {
         this.txid = txid;
         this.storage = storage;
         this.changeHandler = changeHandler;
      }

      @Override
      public boolean validateTextOnChange(String newText, String oldText) {
         return true;
      }

      @Override
      public boolean validateTextOnOk(String newText, String oldText) {
         return true;
      }

      @Override
      public void onNameEntered(String newText, String oldText) {
         storage.storeTransactionLabel(txid, newText);
         if (changeHandler != null) {
            changeHandler.OnTransactionLabelChanged(txid, newText);
         }
      }
   }
}
