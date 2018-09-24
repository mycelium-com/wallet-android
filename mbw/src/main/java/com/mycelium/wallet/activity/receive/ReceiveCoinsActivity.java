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

package com.mycelium.wallet.activity.receive;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.AccountDisplayType;
import com.mycelium.wallet.activity.util.QrImageView;
import com.mycelium.wallet.activity.util.accountstrategy.AccountDisplayStrategy;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExchangeBasedBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExchangeBasedBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExchangeBasedCurrencyValue;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ReceiveCoinsActivity extends Activity {
    private static final String LAST_ADDRESS_BALANCE = "lastAddressBalance";
    private static final String RECEIVING_SINCE = "receivingSince";
    private static final String AMOUNT = "amountData";
    public static final String SYNC_ERRORS = "syncErrors";
    private static final int MAX_SYNC_ERRORS = 8;

    @BindView(R.id.tvAmountLabel) TextView tvAmountLabel;
    @BindView(R.id.tvAmount) TextView tvAmount;
    @BindView(R.id.tvAmountFiat) TextView tvAmountFiat;
    @BindView(R.id.tvWarning) TextView tvWarning;
    @BindView(R.id.tvTitle) TextView tvTitle;
    @BindView(R.id.tvAddress1) TextView tvAddress1;
    @BindView(R.id.tvAddress2) TextView tvAddress2;
    @BindView(R.id.tvAddress3) TextView tvAddress3;
    @BindView(R.id.ivNfc) ImageView ivNfc;
    @BindView(R.id.ivQrCode) QrImageView ivQrCode;
    @BindView(R.id.btShare) Button btShare;

    private MbwManager _mbwManager;
    private Address _address;
    private boolean _havePrivateKey;
    private CurrencyValue _amount;
    private Long _receivingSince;
    private CurrencyValue _lastAddressBalance;
    private int _syncErrors = 0;
    private boolean _showIncomingUtxo;
    private AccountDisplayStrategy accountDisplayStrategy;
    private AccountDisplayType accountDisplayType;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receive_coins_activity);
        ButterKnife.bind(this);

        _mbwManager = MbwManager.getInstance(getApplication());

        // Get intent parameters
        _address = Preconditions.checkNotNull((Address) getIntent().getSerializableExtra("address"));
        _havePrivateKey = getIntent().getBooleanExtra("havePrivateKey", false);
        _showIncomingUtxo = getIntent().getBooleanExtra("showIncomingUtxo", false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {
        updateAmount();
    }

    private void updateAmount() {
        if (!CurrencyValue.isNullOrZero(_amount)) {
            WalletAccount account = _mbwManager.getSelectedAccount();
            CurrencyValue primaryAmount = _amount;
            CurrencyValue alternativeAmount;
            if (primaryAmount.getCurrency().equals(account.getAccountDefaultCurrency())) {
                if (primaryAmount.isBtc() || primaryAmount.isBch()
                        || _mbwManager.getColuManager().isColuAsset(primaryAmount.getCurrency())) {
                    // if the accounts default currency is BTC and the user entered BTC, use the current
                    // selected fiat as alternative currency
                    alternativeAmount = CurrencyValue.fromValue(
                                            primaryAmount, _mbwManager.getFiatCurrency(), _mbwManager.getExchangeRateManager()
                                        );
                } else {
                    // if the accounts default currency isn't BTC, use BTC as alternative
                    alternativeAmount = ExchangeBasedBitcoinValue.fromValue(
                                            primaryAmount, _mbwManager.getExchangeRateManager()
                                        );
                }
            } else {
                // use the accounts default currency as alternative
                alternativeAmount = CurrencyValue.fromValue(
                                        primaryAmount, account.getAccountDefaultCurrency(), _mbwManager.getExchangeRateManager()
                                    );
            }
            if (CurrencyValue.isNullOrZero(alternativeAmount)) {
                tvAmountFiat.setVisibility(GONE);
            } else {
                // show the alternative amountData
                String alternativeAmountString = accountDisplayStrategy.getFormattedValue(alternativeAmount);

                if (!alternativeAmount.isBtc()) {
                    // if the amountData is not in BTC, show a ~ to inform the user, its only approximate and depends
                    // on a FX rate
                    alternativeAmountString = "~ " + alternativeAmountString;
                }

                tvAmountFiat.setText(alternativeAmountString);
                tvAmountFiat.setVisibility(VISIBLE);
            }
        }
    }
}
