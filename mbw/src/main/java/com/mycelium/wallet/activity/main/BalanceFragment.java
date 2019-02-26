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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
import com.mrd.bitlib.crypto.BipSss;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.BipSsImportActivity;
import com.mycelium.wallet.activity.HandleUrlActivity;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.pop.PopActivity;
import com.mycelium.wallet.activity.receive.ReceiveCoinsActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;
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
import com.mycelium.wallet.modularisation.BCHHelper;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.PrivateColuAccount;
import com.mycelium.wapi.wallet.colu.PublicColuAccount;
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
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAddress;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAssetUri;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getBitIdRequest;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getHdKeyNode;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getPopRequest;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getPrivateKey;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getShare;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getUri;

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
         ExchangeRate exchangeRate = exchangeRateManager.getExchangeRate(_mbwManager.getFiatCurrency().getSymbol(), source);
         String price = exchangeRate == null || exchangeRate.price == null ? "not available"
                 : new BigDecimal(exchangeRate.price).setScale(2, BigDecimal.ROUND_DOWN).toPlainString() + " " + _mbwManager.getFiatCurrency().getSymbol();
         String item;
         if (_mbwManager.getSelectedAccount() instanceof PublicColuAccount) {
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
       MbwManager.getEventBus().register(this);
       _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
       if (_exchangeRatePrice == null) {
           _mbwManager.getExchangeRateManager().requestRefresh();
       }

       _tcdFiatDisplay.setCurrencySwitcher(_mbwManager.getCurrencySwitcher());

       updateUi();
       super.onStart();
   }

   @Override
   public void onStop() {
      MbwManager.getEventBus().unregister(this);
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
            if (account instanceof PrivateColuAccount && ((PrivateColuAccount) account).getAccountBalance().getSpendable().value == 0) {
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
        ReceiveCoinsActivity.callMe(getActivity(), _mbwManager.getSelectedAccount(),
                    _mbwManager.getSelectedAccount().canSpend(), true);
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

    private void updateUi() {
        if (!isAdded() || _mbwManager.getSelectedAccount().isArchived()) {
            return;
        }

        WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
        _root.findViewById(R.id.pbProgress).setVisibility(account.isSynchronizing() ? View.VISIBLE : View.GONE);
        updateUiKnownBalance(Preconditions.checkNotNull(account.getAccountBalance()));

        TextView tvBtcRate = _root.findViewById(R.id.tvBtcRate);
        // Set BTC rate
        if (!_mbwManager.hasFiatCurrency()) {
            // No fiat currency selected by user
            tvBtcRate.setVisibility(View.INVISIBLE);
            exchangeSourceLayout.setVisibility(View.GONE);
        } else {
            Value value = _mbwManager.getExchangeRateManager().get(account.getCoinType().oneCoin(), _mbwManager.getFiatCurrency());
            if (value == null) {
                // We have no price, exchange not available
                tvBtcRate.setText(getResources().getString(R.string.exchange_source_not_available
                        , _mbwManager.getExchangeRateManager().getCurrentExchangeSourceName()));
            } else {
                tvBtcRate.setText(getResources().getString(R.string.btc_rate
                        , _mbwManager.getFiatCurrency().getSymbol()
                        , ValueExtensionsKt.toString(value)));
            }
            tvBtcRate.setVisibility(View.VISIBLE);
            exchangeSource.setText(_mbwManager.getExchangeRateManager().getCurrentExchangeSourceName());
            exchangeSourceLayout.setVisibility(View.VISIBLE);
        }
    }

   private void updateUiKnownBalance(Balance balance) {
      CharSequence valueString = ValueExtensionsKt.toStringWithUnit(balance.getSpendable(), _mbwManager.getDenomination());
      ((TextView) _root.findViewById(R.id.tvBalance)).setText(valueString);
      // Show alternative values
      _tcdFiatDisplay.setFiatOnly(true);
      _tcdFiatDisplay.setValue(balance.confirmed);

      // Show/Hide Receiving
      if (balance.pendingReceiving.isPositive()) {
         String receivingString = ValueExtensionsKt.toStringWithUnit(balance.pendingReceiving, _mbwManager.getDenomination());
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
      if (balance.getSendingToForeignAddresses().isPositive()) {
         String sendingString = ValueExtensionsKt.toStringWithUnit(balance.getSendingToForeignAddresses(), _mbwManager.getDenomination());
         String sendingText = getResources().getString(R.string.sending, sendingString);
         TextView tvSending = _root.findViewById(R.id.tvSending);
         tvSending.setText(sendingText);
         tvSending.setVisibility(View.VISIBLE);
      } else {
         _root.findViewById(R.id.tvSending).setVisibility(View.GONE);
      }
      // show fiat value (if balance is in btc)
      setFiatValue(R.id.tvSendingFiat, balance.pendingSending, true);

      // set exchange item
      exchangeSource.setText(_mbwManager.getExchangeRateManager().getCurrentExchangeSourceName());
   }

   private void setFiatValue(int textViewResourceId, Value value, boolean hideOnZeroBalance) {
      TextView tv = _root.findViewById(textViewResourceId);
      if (!_mbwManager.hasFiatCurrency() || _exchangeRatePrice == null
            || (hideOnZeroBalance && value.isZero())) {
         tv.setVisibility(View.GONE);
      } else {
          try {
              long satoshis = value.value;
              tv.setVisibility(View.VISIBLE);
              String converted = Utils.getFiatValueAsString(satoshis, _exchangeRatePrice);
              String currency = _mbwManager.getFiatCurrency().getSymbol();
              tv.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
          } catch (IllegalArgumentException ex) {
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
                switch (type) {
                    case PRIVATE_KEY:
                        InMemoryPrivateKey key = getPrivateKey(data);
                        UUID account = _mbwManager.createOnTheFlyAccount(key);
                        //we dont know yet where at what to send
                        SendInitializationActivity.callMeWithResult(getActivity(), account, true,
                                StringHandlerActivity.SEND_INITIALIZATION_CODE);
                        break;
                    case ADDRESS:
                        GenericAddress address = getAddress(data);
                        startActivity(SendMainActivity.getIntent(getActivity()
                                , _mbwManager.getSelectedAccount().getId(), 0, address, false)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        break;
                    case ASSET_URI: {
                        GenericAssetUri uri = getAssetUri(data);
                        startActivity(SendMainActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), uri, false)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        break;
                    }
                    case HD_NODE:
                        HdKeyNode hdKeyNode = getHdKeyNode(data);
                        if (hdKeyNode.isPrivateHdKeyNode()) {
                            //its an xPriv, we want to cold-spend from it
                            final WalletManager tempWalletManager = _mbwManager.getWalletManager(true);
                            UUID acc = tempWalletManager.createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKeyNode))).get(0);
                            tempWalletManager.setActiveAccount(acc);
                            SendInitializationActivity.callMeWithResult(getActivity(), acc, true,
                                    StringHandlerActivity.SEND_INITIALIZATION_CODE);
                        } else {
                            //its xPub, we want to send to it
                            Intent intent = SendMainActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), hdKeyNode);
                            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                            startActivity(intent);
                        }
                        break;
                    case SHARE:
                        BipSss.Share share = getShare(data);
                        BipSsImportActivity.callMe(getActivity(), share, StringHandlerActivity.IMPORT_SSS_CONTENT_CODE);
                        break;
                    case URI:
                        // open HandleUrlActivity and let it decide what to do with this URL (check if its a payment request)
                        Uri uri = getUri(data);
                        startActivity(HandleUrlActivity.getIntent(getActivity(), uri));
                        break;
                    case POP_REQUEST:
                        PopRequest popRequest = getPopRequest(data);
                        startActivity(new Intent(getActivity(), PopActivity.class)
                                .putExtra("popRequest", popRequest)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        break;
                    case BIT_ID_REQUEST:
                        BitIDSignRequest request = getBitIdRequest(data);
                        BitIDAuthenticationActivity.callMe(getActivity(), request);
                        break;
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
