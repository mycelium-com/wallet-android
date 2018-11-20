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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;
import com.mycelium.wallet.activity.util.ValueExtentionsKt;
import com.mycelium.wallet.content.HandleConfigFactory;
import com.mycelium.wallet.content.ResultType;
import com.mycelium.wallet.content.StringHandleConfig;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.ExchangeSourceChanged;
import com.mycelium.wallet.event.RefreshingExchangeRatesFailed;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.exchange.ExchangeRateManager;
import com.mycelium.wallet.modularisation.BCHHelper;
import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.colu.ColuPubOnlyAccount;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class BalanceFragment extends Fragment {
   public static final String COINMARKETCAP = "Coinmarketcap";
   public static final int GENERIC_SCAN_REQUEST = 4;

   private MbwManager _mbwManager;
   private View _root;
   private Double _exchangeRatePrice;
   private Toaster _toaster;
   @BindView(R.id.tcdFiatDisplay) ToggleableCurrencyButton _tcdFiatDisplay;

   @BindView(R.id.exchangeSource) TextView exchangeSource;
   @BindView(R.id.exchangeSourceLayout) View exchangeSourceLayout;


   @Override
   public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_balance_view, container, false));
      final View balanceArea = Preconditions.checkNotNull(_root.findViewById(R.id.llBalance));
      balanceArea.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            _mbwManager.getWalletManager(false).startSynchronization();
         }
      });
      ButterKnife.bind(this, _root);
      updateExchangeSourceMenu();
      return _root;
   }

   private void updateExchangeSourceMenu() {
      final PopupMenu exchangeMenu = new PopupMenu(getActivity(), exchangeSourceLayout);
      exchangeSourceLayout.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            exchangeMenu.show();
         }
      });

      ExchangeRateManager exchangeRateManager = _mbwManager.getExchangeRateManager();
      final List<String> sources = exchangeRateManager.getExchangeSourceNames();
      final Map<String, String> sourcesAndValues = new HashMap<>(); // Needed for popup menu

      Collections.sort(sources, new Comparator<String>() {
         @Override
         public int compare(String rate1, String rate2) {
            return rate1.compareToIgnoreCase(rate2);
         }
      });

      for (int i = 0; i < sources.size(); i++) {
         String source = sources.get(i);
         ExchangeRate exchangeRate = exchangeRateManager.getExchangeRate(_mbwManager.getFiatCurrency(), source);
         String price = exchangeRate == null || exchangeRate.price == null ? "not available"
                 : new BigDecimal(exchangeRate.price).setScale(2, BigDecimal.ROUND_DOWN).toPlainString() + " " + _mbwManager.getFiatCurrency();
         String item;
         if (_mbwManager.getSelectedAccount() instanceof ColuPubOnlyAccount) {
            item = COINMARKETCAP + "/" + source;
         } else {
            item = source + " (" + price + ")";
         }

         sourcesAndValues.put(item, source);
         exchangeMenu.getMenu().add(item);
      }

      exchangeMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
         @Override
         public boolean onMenuItemClick(MenuItem item) {
            _mbwManager.getExchangeRateManager().setCurrentExchangeSourceName(sourcesAndValues.get(item.getTitle().toString()));
            return false;
         }
      });
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(true);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Context context) {
      _mbwManager = MbwManager.getInstance(context);
      _toaster = new Toaster(this);
      super.onAttach(context);
   }

   @Override
   public void onStart() {
      _mbwManager.getEventBus().register(this);
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      if (_exchangeRatePrice == null) {
         _mbwManager.getExchangeRateManager().requestRefresh();
      }

      _tcdFiatDisplay.setCurrencySwitcher(_mbwManager.getCurrencySwitcher());
      _tcdFiatDisplay.setEventBus(_mbwManager.getEventBus());

      updateUi();
      super.onStart();
   }

   @Override
   public void onStop() {
      _mbwManager.getEventBus().unregister(this);
      super.onStop();
   }

    @OnClick(R.id.btSend)
    void onClickSend() {
        if (isBCH()) {
            BCHHelper.bchTechnologyPreviewDialog(getActivity());
            return;
        }
        WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
        if (account.canSpend()) {
            if (account instanceof ColuPubOnlyAccount && ((ColuAccount) account).getAccountBalance().getSpendable().value == 0) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.rmc_send_warning, account.getCoinType().getName()))
                        .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), _mbwManager.getSelectedAccount().getId(), false);
                            }
                        })
                        .create()
                        .show();
            } else {
                SendInitializationActivity.callMe(BalanceFragment.this.getActivity(), _mbwManager.getSelectedAccount().getId(), false);
            }
        } else {
            new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.this_is_read_only_account)
                    .setPositiveButton(R.string.button_ok, null).create().show();

        }
    }

    @OnClick(R.id.btReceive)
    void onClickReceive() {
        //todo: generic address check
        //Address receivingAddress = Address.fromString(_mbwManager.getSelectedAccount().getReceiveAddress().toString());
        //if(receivingAddress != null) {
        ReceiveCoinsActivity.callMe(getActivity(), _mbwManager.getSelectedAccount(),
                    _mbwManager.getSelectedAccount().canSpend(), true);
        //}
    }

    @OnClick(R.id.btScan)
    void onClickScan() {
        if (isBCH()) {
            BCHHelper.bchTechnologyPreviewDialog(getActivity());
            return;
        }
        //perform a generic scan, act based upon what we find in the QR code
        StringHandleConfig config = HandleConfigFactory.genericScanRequest();
        ScanActivity.callMe(this, GENERIC_SCAN_REQUEST, config);
    }

    private boolean isBCH() {
        return _mbwManager.getSelectedAccount() instanceof Bip44BCHAccount
               || _mbwManager.getSelectedAccount() instanceof SingleAddressBCHAccount;
    }

    @SuppressLint("SetTextI18n")
    private void updateUi() {
        if (!isAdded()) {
            return;
        }
        if (_mbwManager.getSelectedAccount().isArchived()) {
            return;
        }

        WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
        updateUiKnownBalance(Preconditions.checkNotNull(account.getAccountBalance()));
   }

   private void updateUiKnownBalance(Balance balance) {

      CharSequence valueString = ValueExtentionsKt.toStringWithUnit(balance.confirmed, _mbwManager.getBitcoinDenomination());
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(valueString);
// TODO remove or change isSynchronizing call and uncomment
//      _root.findViewById(R.id.pbProgress).setVisibility(balance.isSynchronizing ? View.VISIBLE : View.GONE);
      // Show alternative values
      _tcdFiatDisplay.setFiatOnly(true);
      _tcdFiatDisplay.setValue(balance.confirmed);

      // Show/Hide Receiving
      if (balance.pendingReceiving.isPositive()) {
         String receivingString = ValueExtentionsKt.toStringWithUnit(balance.pendingReceiving, _mbwManager.getBitcoinDenomination());
         String receivingText = getResources().getString(R.string.receiving, receivingString);
         TextView tvReceiving = _root.findViewById(R.id.tvReceiving);
         tvReceiving.setText(receivingText);
         tvReceiving.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvReceiving).setVisibility(View.GONE);
      }
      // show fiat value (if balance is in btc)
      setFiatValue(R.id.tvReceivingFiat, balance.pendingReceiving, true);

      // Show/Hide Sending
      if (balance.pendingSending.isPositive()) {
         String sendingString = ValueExtentionsKt.toStringWithUnit(balance.pendingSending, _mbwManager.getBitcoinDenomination());
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = _root.findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvSending).setVisibility(View.GONE);
      }
      // show fiat value (if balance is in btc)
      setFiatValue(R.id.tvSendingFiat, balance.pendingSending, true);
   }

   private void setFiatValue(int textViewResourceId, Value value, boolean hideOnZeroBalance) {
      TextView tv = _root.findViewById(textViewResourceId);
      if (!_mbwManager.hasFiatCurrency()
            || _exchangeRatePrice == null
            || (hideOnZeroBalance && value.isZero())
//            || value.isFiat()
            ) {
         tv.setVisibility(View.GONE);
      } else {
         try {
            tv.setVisibility(View.VISIBLE);

            Value converted = _mbwManager.getExchangeRateManager().get(value, _mbwManager.getFiatCurrency());
            tv.setText(converted != null ? ValueExtentionsKt.toStringWithUnit(converted, _mbwManager.getBitcoinDenomination()) : null);
         } catch (Exception ex) {
            // something failed while calculating the bitcoin amount
            tv.setVisibility(View.GONE);
         }
      }
   }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GENERIC_SCAN_REQUEST) {
            if (resultCode != RESULT_OK) {
                //report to user in case of error
                //if no scan handlers match successfully, this is the last resort to display an error msg
                ScanActivity.toastScanError(resultCode, data, getActivity());
            } else {
                ResultType type = (ResultType) data.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
                if (type == ResultType.PRIVATE_KEY) {
                    InMemoryPrivateKey key = StringHandlerActivity.getPrivateKey(data);
                    UUID account = _mbwManager.createOnTheFlyAccount(key);
                    //we dont know yet where at what to send
                    BitcoinUri uri = new BitcoinUri(null, null, null);
                    SendInitializationActivity.callMeWithResult(getActivity(), account, uri, true,
                            StringHandlerActivity.SEND_INITIALIZATION_CODE);
                } else if (type == ResultType.ADDRESS) {
                    GenericAddress address = StringHandlerActivity.getAddress(data);
                    Intent intent = SendMainActivity.getIntent(getActivity()
                            , _mbwManager.getSelectedAccount().getId(), null, address, false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(intent);
                } else if (type == ResultType.URI) {
                    GenericAssetUri uri = StringHandlerActivity.getUri(data);
                    Intent intent = SendMainActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), uri, false);
                    intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                    startActivity(intent);
                }
            }
        }
    }

   @Subscribe
   public void refreshingExchangeRatesFailed(RefreshingExchangeRatesFailed event){
      _toaster.toastConnectionError();
      _exchangeRatePrice = null;
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event){
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
      updateExchangeSourceMenu();
   }

   @Subscribe
   public void exchangeSourceChanged(ExchangeSourceChanged event) {
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
   }

   @Subscribe
   public void selectedCurrencyChanged(SelectedCurrencyChanged event){
      _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
      updateUi();
      updateExchangeSourceMenu();
   }

   /**
    * The selected Account changed, update UI to reflect other BalanceSatoshis
    */
   @Subscribe
   public void selectedAccountChanged(SelectedAccountChanged event) {
      updateUi();
      updateExchangeSourceMenu();
   }

   /**
    * balance has changed, update UI
    */
   @Subscribe
   public void balanceChanged(BalanceChanged event) {
      updateUi();
   }

   @Subscribe
   public void accountChanged(AccountChanged event) {
      updateUi();
   }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      updateUi();
   }
}
