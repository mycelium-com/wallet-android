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

package com.mycelium.wallet.activity.main;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wallet.BitcoinUriWithAddress;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivityN;
import com.mycelium.wallet.activity.util.QrImageView;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.ReceivingAddressChanged;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

public class AddressFragment extends Fragment {

   private View _root;
   private MbwManager _mbwManager;
   private boolean _showBip44Path;
   @BindView(R.id.ivQR)
   QrImageView qrButton;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = Preconditions.checkNotNull(inflater.inflate(R.layout.address_view, container, false));
      ButterKnife.bind(this, _root);
      qrButton.setTapToCycleBrightness(false);
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(true);
      _mbwManager = MbwManager.getInstance(getActivity());
      _showBip44Path = _mbwManager.getMetadataStorage().getShowBip44Path();
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onResume() {
      getEventBus().register(this);
      updateUi();
      super.onResume();
   }

   @Override
   public void onPause() {
      getEventBus().unregister(this);
      super.onPause();
   }

   private Bus getEventBus() {
      return _mbwManager.getEventBus();
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      WalletAccount account = _mbwManager.getSelectedAccount();
      if (account.isArchived()) {
         return;
      }

      // Update QR code

      Optional<Address> receivingAddress = getAddress();

      // Update address
      if (receivingAddress.isPresent()) {
         // Set address
         qrButton.setVisibility(View.VISIBLE);
         String address = receivingAddress.get().toString();
         qrButton.setQrCode(BitcoinUriWithAddress.fromAddress(receivingAddress.get()).toString());
         ((TextView) _root.findViewById(R.id.tvAddress)).setText(address);
         if (_showBip44Path && receivingAddress.get().getBip32Path() != null) {
            HdKeyPath path = receivingAddress.get().getBip32Path();
            ((TextView) _root.findViewById(R.id.tvAddressPath)).setText(path.toString());
         } else {
            ((TextView) _root.findViewById(R.id.tvAddressPath)).setText("");
         }
      } else {
         // No address available
         qrButton.setVisibility(View.INVISIBLE);
         ((TextView) _root.findViewById(R.id.tvAddress)).setText("");
         ((TextView) _root.findViewById(R.id.tvAddressPath)).setText("");
      }

      // Show name of bitcoin address according to address book
      TextView tvAddressTitle = (TextView) _root.findViewById(R.id.tvAddressLabel);
      ImageView ivAccountType = (ImageView) _root.findViewById(R.id.ivAccountType);

      String name = _mbwManager.getMetadataStorage().getLabelByAccount(account.getId());
      if(account.getType() == WalletAccount.Type.BCHSINGLEADDRESS
              || account.getType() == WalletAccount.Type.BCHBIP44) {
         name = getString(R.string.bitcoin_cash) + " - " + name;
      }
      if (name.length() == 0) {
         tvAddressTitle.setVisibility(View.GONE);
         ivAccountType.setVisibility(View.GONE);
      } else {
         tvAddressTitle.setVisibility(View.VISIBLE);
         tvAddressTitle.setText(Html.fromHtml(name));

         // show account type icon next to the name
         Drawable drawableForAccount = Utils.getDrawableForAccount(account, true, getResources());
         if (drawableForAccount == null) {
            ivAccountType.setVisibility(View.GONE);
         } else {
            ivAccountType.setImageDrawable(drawableForAccount);
            ivAccountType.setVisibility(View.VISIBLE);
         }
      }

   }

   @OnClick(R.id.address_layout)
   void addressClick() {
      final Optional<Address> address = getAddress();
      if (address.isPresent()) {
         Utils.setClipboardString(address.get().toString(), getActivity());
         Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
      }
   }

   public Optional<Address> getAddress() {
      return _mbwManager.getSelectedAccount().getReceivingAddress();
   }

   @OnClick(R.id.ivQR)
   void qrClick() {
      WalletAccount account = _mbwManager.getSelectedAccount();
      if (account.getReceivingAddress().isPresent()) {
         ReceiveCoinsActivityN.callMe(getActivity(), account, account.canSpend());
      }
   }

   /**
    * We got a new Receiving Address, either because the selected Account changed,
    * or because our HD Account received Coins and changed the Address
    */
   @Subscribe
   public void receivingAddressChanged(ReceivingAddressChanged event) {
      updateUi();
   }

   @Subscribe
   public void accountChanged(AccountChanged event) {
      updateUi();
   }

   @Subscribe
   public void balanceChanged(BalanceChanged event) {
      updateUi();
   }

}
