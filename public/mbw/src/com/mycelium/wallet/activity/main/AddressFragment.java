package com.mycelium.wallet.activity.main;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;

public class AddressFragment extends Fragment {

   public interface AddressFragmentContainer {

      public MbwManager getMbwManager();

      public void addObserver(WalletFragmentObserver observer);

      public void removeObserver(WalletFragmentObserver observer);

   }

   public static AddressFragment newInstance(Record record, int position, int addresses) {
      AddressFragment fragment = new AddressFragment();
      Bundle args = new Bundle();
      args.putSerializable("record", record);
      args.putInt("position", position);
      args.putInt("addresses", addresses);
      fragment.setArguments(args);
      return fragment;
   }

   private AddressFragmentContainer _container;
   private MbwManager _mbwManager;
   private AddressBookManager _addressBook;
   private View _root;
   private int _globalLayoutHeight;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = (ViewGroup) inflater.inflate(R.layout.main_address_view, container, false);

      return _root;
   }

   @Override
   public void onResume() {
      _container = (AddressFragmentContainer) this.getActivity();
      _mbwManager = _container.getMbwManager();
      _addressBook = _mbwManager.getAddressBookManager();

      // Show small QR code once the layout has completed
      ImageView qrImage = (ImageView) _root.findViewById(R.id.ivQR);
      qrImage.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

         @Override
         public void onGlobalLayout() {
            int height = ((ImageView) _root.findViewById(R.id.ivQR)).getHeight();
            // Guard to prevent us from updating more than once
            if (_globalLayoutHeight == height) {
               return;
            }
            _globalLayoutHeight = height;
            updateQrImage();
         }
      });

      // Show large QR code when clicking small qr code
      qrImage.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            ReceiveCoinsActivity.callMe(AddressFragment.this.getActivity(), getRecord());
         }
      });

      _root.findViewById(R.id.llAddress).setOnClickListener(addressClickListener);
      updateUi();
      super.onResume();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }

   @Override
   public void onPause() {
      super.onPause();
   }

   public Record getRecord() {
      return (Record) getArguments().getSerializable("record");
   }

   public int getPosition() {
      return getArguments().getInt("position");
   }

   public int getNumAddresses() {
      return getArguments().getInt("addresses");
   }

   private final OnClickListener addressClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         Intent intent = new Intent(Intent.ACTION_SEND);
         intent.setType("text/plain");
         intent.putExtra(Intent.EXTRA_TEXT, getRecord().address.toString());
         startActivity(Intent.createChooser(intent, getString(R.string.share_bitcoin_address)));
      }
   };

   private void updateUi() {
      if (!isAdded()) {
         return;
      }

      boolean isLeftMost = getPosition() == 0;
      boolean isRightMost = getPosition() == getNumAddresses() - 1;
      Resources resources = getActivity().getResources();
      if (isLeftMost && isRightMost) {
         _root.findViewById(R.id.llTop).setBackgroundDrawable(resources.getDrawable(R.drawable.addr_box_full));
      } else if (isLeftMost) {
         _root.findViewById(R.id.llTop).setBackgroundDrawable(resources.getDrawable(R.drawable.addr_box_left));
      } else if (isRightMost) {
         _root.findViewById(R.id.llTop).setBackgroundDrawable(resources.getDrawable(R.drawable.addr_box_right));
      } else {
         _root.findViewById(R.id.llTop).setBackgroundDrawable(resources.getDrawable(R.drawable.addr_box_center));
      }
      Address address = getRecord().address;
      // Show name of bitcoin address according to address book
      TextView tvAddressTitle = (TextView) _root.findViewById(R.id.tvAddressLabel);
      String name = _addressBook.getNameByAddress(address.toString());
      if (name.length() == 0) {
         tvAddressTitle.setText(R.string.your_bitcoin_address);
         tvAddressTitle.setGravity(Gravity.LEFT);
      } else {
         tvAddressTitle.setText(name);
         tvAddressTitle.setGravity(Gravity.CENTER_HORIZONTAL);
         tvAddressTitle.setGravity(Gravity.LEFT);
      }


      // Set address
      String[] addressStrings = Utils.stringChopper(address.toString(), 12);
      ((TextView) _root.findViewById(R.id.tvAddress1)).setText(addressStrings[0]);
      ((TextView) _root.findViewById(R.id.tvAddress2)).setText(addressStrings[1]);
      ((TextView) _root.findViewById(R.id.tvAddress3)).setText(addressStrings[2]);

   }

   private void updateQrImage() {
      if (_globalLayoutHeight == 0) {
         return;
      }
      ImageView qrImage = (ImageView) _root.findViewById(R.id.ivQR);
      int margin = 5;
      Bitmap qrCode = Utils.getQRCodeBitmap("bitcoin:" + getRecord().address.toString(), _globalLayoutHeight, margin);
      qrImage.setImageBitmap(qrCode);
   }

}
