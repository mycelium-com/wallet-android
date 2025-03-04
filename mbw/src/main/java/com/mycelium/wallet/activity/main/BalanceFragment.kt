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
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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
import com.mycelium.wallet.Constants;
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
import com.mycelium.wallet.activity.send.SendCoinsActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
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
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.content.AssetUri;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.ColuAccount;
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
                WalletAccount<?> account = _mbwManager.getSelectedAccount();
                _mbwManager.getWalletManager(false)
                        .startSynchronization(SyncMode.NORMAL_FORCED, Collections.singletonList(account));
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
        WalletAccount selectedAccount = _mbwManager.getSelectedAccount();
        final List<String> sources = exchangeRateManager.getExchangeSourceNames(selectedAccount.getCoinType().getSymbol());
        final Map<String, String> sourcesAndValues = new HashMap<>(); // Needed for popup menu

        Collections.sort(sources, new Comparator<String>() {
            @Override
            public int compare(String rate1, String rate2) {
                return rate1.compareToIgnoreCase(rate2);
            }
        });

        for (int i = 0; i < sources.size(); i++) {
            String source = sources.get(i);
            ExchangeRate exchangeRate = exchangeRateManager.getExchangeRate(selectedAccount.getCoinType().getSymbol(),
                    _mbwManager.getFiatCurrency(selectedAccount.getCoinType()).getSymbol(), source);
            String price = exchangeRate == null || exchangeRate.price == null ? "not available"
                    : new BigDecimal(exchangeRate.price).setScale(2, BigDecimal.ROUND_DOWN).toPlainString() +
                    " " + _mbwManager.getFiatCurrency(selectedAccount.getCoinType()).getSymbol();
            String item;
            if (selectedAccount instanceof ColuAccount) {
                item = COINMARKETCAP + "/" + source;
            } else {
                item = source + " (" + price + ")";
            }
            sourcesAndValues.put(item, source);
            exchangeMenu.getMenu().add(item);
        }

        // if we ended up with not existent source name for current cryptocurrency (CC)
        // after we have switched accounts for different CC
        // then use the default exchange
        if (sources.size() != 0 && !sources.contains(exchangeRateManager.getCurrentExchangeSourceName(selectedAccount.getCoinType().getSymbol()))) {
            exchangeRateManager.setCurrentExchangeSourceName(selectedAccount.getCoinType().getSymbol(), Constants.DEFAULT_EXCHANGE);
        }

        exchangeMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                exchangeRateManager.setCurrentExchangeSourceName(selectedAccount.getCoinType().getSymbol(),
                        sourcesAndValues.get(item.getTitle().toString()));
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
        _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice(_mbwManager.getSelectedAccount().getCoinType());
        if (_exchangeRatePrice == null) {
            _mbwManager.getExchangeRateManager().requestRefresh();
        }

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
            return;
        }
        WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
        if (account.canSpend()) {
            if (account instanceof ColuAccount && ((ColuAccount) account).getAccountBalance().getSpendable().equalZero()) {
                new AlertDialog.Builder(getActivity())
                        .setMessage(getString(R.string.rmc_send_warning, account.getCoinType().getName()))
                        .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SendInitializationActivity.callMe(BalanceFragment.this.requireActivity(), _mbwManager.getSelectedAccount().getId(), false);
                            }
                        })
                        .create()
                        .show();
            } else {
                SendInitializationActivity.callMe(BalanceFragment.this.requireActivity(), _mbwManager.getSelectedAccount().getId(), false);
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
        _tcdFiatDisplay.setCoinType(account.getCoinType());
        updateUiKnownBalance(Preconditions.checkNotNull(account.getAccountBalance()), account.getCoinType());

        TextView tvBtcRate = _root.findViewById(R.id.tvBtcRate);

        // Set BTC rate
        if (!_mbwManager.hasFiatCurrency()) {
            // No fiat currency selected by user
            tvBtcRate.setVisibility(View.INVISIBLE);
            exchangeSourceLayout.setVisibility(View.GONE);
        } else {
            Value value = _mbwManager.getExchangeRateManager().get(account.getCoinType().oneCoin(),
                    _mbwManager.getFiatCurrency(account.getCoinType()));
            String exchange = _mbwManager.getExchangeRateManager().getCurrentExchangeSourceName(_mbwManager.getSelectedAccount().getCoinType().getSymbol());
            if (value == null) {
                // We have no price, exchange not available
                // or no exchange rate providers for the account
                if (exchange != null) {
                    tvBtcRate.setText(getResources().getString(R.string.exchange_source_not_available, exchange));
                } else {
                    tvBtcRate.setText(R.string.no_exchange_available);
                }
            } else {
                tvBtcRate.setText(getResources().getString(R.string.balance_rate
                        , account.getCoinType().getSymbol()
                        , _mbwManager.getFiatCurrency(account.getCoinType()).getSymbol()
                        , ValueExtensionsKt.toString(value)));
            }
            tvBtcRate.setVisibility(View.VISIBLE);
            exchangeSource.setText(exchange);
            exchangeSourceLayout.setVisibility(exchange != null ? View.VISIBLE : View.GONE);
        }
    }

    private void updateUiKnownBalance(Balance balance, AssetInfo coinType) {
        CharSequence valueString = ValueExtensionsKt.toStringWithUnit(balance.getSpendable(), _mbwManager.getDenomination(coinType));
        ((TextView) _root.findViewById(R.id.tvBalance)).setText(valueString);

        // Show alternative values
        _tcdFiatDisplay.setValue(balance.getSpendable());

        // Show/Hide Receiving
        if (balance.pendingReceiving.isPositive()) {
            String receivingString = ValueExtensionsKt.toStringWithUnit(balance.pendingReceiving, _mbwManager.getDenomination(coinType));
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
            String sendingString = ValueExtensionsKt.toStringWithUnit(balance.getSendingToForeignAddresses(),
                    _mbwManager.getDenomination(coinType));
            String sendingText = getResources().getString(R.string.sending, sendingString);
            TextView tvSending = _root.findViewById(R.id.tvSending);
            tvSending.setText(sendingText);
            tvSending.setVisibility(View.VISIBLE);
        } else {
            _root.findViewById(R.id.tvSending).setVisibility(View.GONE);
        }
        // show fiat value (if balance is in btc)
        setFiatValue(R.id.tvSendingFiat, balance.getSendingToForeignAddresses(), true);

        // set exchange item
        exchangeSource.setText(_mbwManager.getExchangeRateManager().getCurrentExchangeSourceName(
                _mbwManager.getSelectedAccount().getCoinType().getSymbol()));
    }

    private void setFiatValue(int textViewResourceId, Value value, boolean hideOnZeroBalance) {
        TextView tv = _root.findViewById(textViewResourceId);
        if (!_mbwManager.hasFiatCurrency() || _exchangeRatePrice == null
                || (hideOnZeroBalance && !value.isPositive())) {
            tv.setVisibility(View.GONE);
        } else {
            try {
                Value converted = _mbwManager.getExchangeRateManager().get(value,
                        _mbwManager.getFiatCurrency(_mbwManager.getSelectedAccount().getCoinType()));
                if(converted != null) {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(ValueExtensionsKt.toStringWithUnit(converted));
                } else {
                    tv.setVisibility(View.GONE);
                }
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
                        // ask user what WIF privkey he/she scanned as there are options
                        final int[] selectedItem = new int[1];
                        CharSequence[] choices = new CharSequence[2];
                        choices[0] = "BTC";
                        choices[1] = "FIO";
                        new AlertDialog.Builder(requireActivity())
                                .setTitle("Choose blockchain")
                                .setSingleChoiceItems(choices, 0, (dialogInterface, i) -> selectedItem[0] = i)
                                .setPositiveButton(requireActivity().getString(R.string.ok), (dialogInterface, i) -> {
                                    UUID account;
                                    if (selectedItem[0] == 0) {
                                        account = _mbwManager.createOnTheFlyAccount(key, Utils.getBtcCoinType());
                                    } else {
                                        account = _mbwManager.createOnTheFlyAccount(key, Utils.getFIOCoinType());
                                    }

                                    //we dont know yet where at what to send
                                    SendInitializationActivity.callMeWithResult(requireActivity(), account, true,
                                            StringHandlerActivity.SEND_INITIALIZATION_CODE);
                                })
                                .setNegativeButton(this.getString(R.string.cancel), null)
                                .show();
                        break;
                    case ADDRESS:
                        Address address = getAddress(data);
                        startActivity(SendCoinsActivity.getIntent(getActivity(),
                                _mbwManager.getSelectedAccount().getId(),
                                Value.zeroValue(_mbwManager.getSelectedAccount().getCoinType()),
                                address, false)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        break;
                    case ASSET_URI: {
                        AssetUri uri = getAssetUri(data);
                        startActivity(SendCoinsActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), uri, false)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT));
                        break;
                    }
                    case HD_NODE:
                        HdKeyNode hdKeyNode = getHdKeyNode(data);
                        if (hdKeyNode.isPrivateHdKeyNode()) {
                            //its an xPriv, we want to cold-spend from it
                            final WalletManager tempWalletManager = _mbwManager.getWalletManager(true);
                            UUID acc = tempWalletManager.createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKeyNode))).get(0);
                            tempWalletManager.startSynchronization(acc);
                            SendInitializationActivity.callMeWithResult(getActivity(), acc, true,
                                    StringHandlerActivity.SEND_INITIALIZATION_CODE);
                        } else {
                            //its xPub, we want to send to it
                            Intent intent = SendCoinsActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), hdKeyNode);
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
    public void refreshingExchangeRatesFailed(RefreshingExchangeRatesFailed event) {
        _toaster.toastConnectionError();
        _exchangeRatePrice = null;
    }

    @Subscribe
    public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
        _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice(_mbwManager.getSelectedAccount().getCoinType());
        updateUi();
        updateExchangeSourceMenu();
    }

    @Subscribe
    public void exchangeSourceChanged(ExchangeSourceChanged event) {
        _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice(_mbwManager.getSelectedAccount().getCoinType());
        updateUi();
    }

    @Subscribe
    public void selectedCurrencyChanged(SelectedCurrencyChanged event) {
        _exchangeRatePrice = _mbwManager.getCurrencySwitcher().getExchangeRatePrice(_mbwManager.getSelectedAccount().getCoinType());
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
