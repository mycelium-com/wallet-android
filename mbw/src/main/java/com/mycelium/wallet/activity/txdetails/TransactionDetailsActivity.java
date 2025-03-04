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

package com.mycelium.wallet.activity.txdetails;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay;
import com.mycelium.wallet.activity.util.TransactionDetailsLabel;
import com.mycelium.wapi.wallet.TransactionSummary;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.erc20.ERC20Account;
import com.mycelium.wapi.wallet.eth.EthAccount;
import com.mycelium.wapi.wallet.fio.FioAccount;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

//TODO convert to kotlin with null checking
public class TransactionDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_TXID = "transactionID";
    public static final String ACCOUNT_ID = "accountId";
    public static final LayoutParams FPWC = new LayoutParams(MATCH_PARENT, WRAP_CONTENT, 1);
    public static final LayoutParams WCWC = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
    private boolean coluMode = false;
    private TransactionSummary tx;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_details_activity);

        byte[] txid = getTransactionIdFromIntent();
        WalletManager walletManager = MbwManager.getInstance(this.getApplication()).getWalletManager(false);
        UUID accountId = (UUID) getIntent().getSerializableExtra(ACCOUNT_ID);
        WalletAccount account = walletManager.getAccount(accountId);
        tx = account.getTxSummary(txid);
        coluMode = account instanceof ColuAccount;
        DetailsFragment detailsFragment = (DetailsFragment) getSupportFragmentManager().findFragmentById(R.id.spec_details_fragment);
        if (detailsFragment == null && tx != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (account instanceof EthAccount || account instanceof ERC20Account) {
                transaction.add(R.id.spec_details_fragment, EthDetailsFragment.newInstance(tx));
            } else if (account instanceof FioAccount) {
                transaction.add(R.id.spec_details_fragment, FioDetailsFragment.newInstance(tx));
            } else if(account instanceof BitcoinVaultHdAccount) {
                transaction.add(R.id.spec_details_fragment, BtcvDetailsFragment.newInstance(tx, accountId));
            } else {
                transaction.add(R.id.spec_details_fragment, BtcDetailsFragment.newInstance(tx, coluMode, accountId));
            }
            transaction.commit();
        }
        updateUi();
    }

    private void updateUi() {
        // Set Hash
        TransactionDetailsLabel tvHash = findViewById(R.id.tvHash);
        tvHash.setColuMode(coluMode);
        tvHash.setTransaction(tx);

        // Set Confirmed
        int confirmations = tx.getConfirmations();

        String confirmed;
        if (confirmations > 0) {
            confirmed = getResources().getString(R.string.confirmed_in_block, tx.getHeight());
        } else {
            confirmed = getResources().getString(R.string.no);
        }

        // check if tx is in outgoing queue
        TransactionConfirmationsDisplay confirmationsDisplay = findViewById(R.id.tcdConfirmations);
        TextView confirmationsCount = findViewById(R.id.tvConfirmations);

        if (tx != null && tx.isQueuedOutgoing()) {
            confirmationsDisplay.setNeedsBroadcast();
            confirmationsCount.setText("");
            confirmed = getResources().getString(R.string.transaction_not_broadcasted_info);
        } else {
            confirmationsDisplay.setConfirmations(confirmations);
            confirmationsCount.setText(String.valueOf(confirmations));
        }

        ((TextView) findViewById(R.id.tvConfirmed)).setText(confirmed);

        // Set Date & Time
        Date date = new Date(tx.getTimestamp() * 1000L);
        Locale locale = getResources().getConfiguration().locale;
        DateFormat dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale);
        String dateString = dayFormat.format(date);
        ((TextView) findViewById(R.id.tvDate)).setText(dateString);
        DateFormat hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale);
        String timeString = hourFormat.format(date);
        ((TextView) findViewById(R.id.tvTime)).setText(timeString);
    }

    private byte[] getTransactionIdFromIntent() {
        return getIntent().getByteArrayExtra(EXTRA_TXID);
    }
}
