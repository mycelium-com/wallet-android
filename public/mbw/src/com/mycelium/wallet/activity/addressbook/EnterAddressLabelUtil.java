package com.mycelium.wallet.activity.addressbook;

import android.content.Context;

import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.EnterTextDialog;
import com.mycelium.wallet.R;

public class EnterAddressLabelUtil {

   public interface AddressLabelChangedHandler {
      public void OnAddressLabelChanged(String address, String label);
   }

   public static void enterAddressLabel(Context context, AddressBookManager addressBookManager, String address,
         AddressLabelChangedHandler changeHandler) {
      String hintText = context.getResources().getString(R.string.name);
      String currentName = addressBookManager.getNameByAddress(address);
      currentName = currentName == null ? "" : currentName;
      String invalidOkToastMessage = context.getResources().getString(R.string.address_label_not_unique);
      EnterTextDialog.show(context, R.string.edit_address_label_title, hintText, currentName,
            new EnterAddressLabelHandler(addressBookManager, address, invalidOkToastMessage, changeHandler));

   }

   private static class EnterAddressLabelHandler extends EnterTextDialog.EnterTextHandler {

      private AddressBookManager _addressBook;
      private String _address;
      private String _invalidOkToastMessage;
      private AddressLabelChangedHandler _changeHandler;

      public EnterAddressLabelHandler(AddressBookManager addressBook, String address, String invalidOkToastMessage,
            AddressLabelChangedHandler changeHandler) {
         _address = address;
         _addressBook = addressBook;
         _invalidOkToastMessage = invalidOkToastMessage;
         _changeHandler = changeHandler;
      }

      @Override
      public boolean validateTextOnChange(String newText, String oldText) {
         return true;
      };

      @Override
      public boolean validateTextOnOk(String newText, String oldText) {
         // Make sure that no address exists with that name, or that we are
         // updating the existing entry with the same name. It is OK for the
         // name to be empty, in which case it will get deleted
         String existingAddress = _addressBook.getAddressByName(newText);
         return existingAddress == null || existingAddress.equals(_address);

      };

      @Override
      public boolean getVibrateOnInvalidOk(String newText, String oldText) {
         return true;
      };

      @Override
      public String getToastTextOnInvalidOk(String newText, String oldText) {
         return _invalidOkToastMessage;
      };

      @Override
      public void onNameEntered(String newText, String oldText) {
         // No address exists with that name, or we are updating the
         // existing entry with the same name. If the name is blank the
         // entry will get deleted
         _addressBook.insertUpdateOrDeleteEntry(_address, newText);
         if (_changeHandler != null) {
            _changeHandler.OnAddressLabelChanged(_address, newText);
         }
      }
   }

}
