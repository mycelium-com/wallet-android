/*
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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.AccountCreated;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.erc20.ERC20Config;
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token;
import com.mycelium.wapi.wallet.eth.EthAccount;
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig;
import com.mycelium.wapi.wallet.eth.EthereumModule;
import com.mycelium.wapi.wallet.eth.EthereumModuleKt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class AddAccountActivity extends Activity {
    private ETHCreationAsyncTask ethCreationAsyncTask;

    public static void callMe(Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), AddAccountActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static final String RESULT_KEY = "account";
    public static final String RESULT_MSG = "result_msg";
    public static final String IS_UPGRADE = "account_upgrade";

    private static final int IMPORT_SEED_CODE = 0;
    private static final int ADD_ADVANCED_CODE = 1;
    private Toaster _toaster;
    private MbwManager _mbwManager;
    private ProgressDialog _progress;
    private int selectedIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_account_activity);
        ButterKnife.bind(this);
        _mbwManager = MbwManager.getInstance(this);
        _toaster = new Toaster(this);

        findViewById(R.id.btAdvanced).setOnClickListener(advancedClickListener);
        findViewById(R.id.btHdCreate).setOnClickListener(createHdAccount);
        if (_mbwManager.getMetadataStorage().getMasterSeedBackupState() == MetadataStorage.BackupState.VERIFIED) {
            findViewById(R.id.tvWarningNoBackup).setVisibility(View.GONE);
        } else {
            findViewById(R.id.tvInfoBackup).setVisibility(View.GONE);
        }
        final View coluCreate = findViewById(R.id.btColuCreate);
        coluCreate.setOnClickListener(createColuAccount);
        _progress = new ProgressDialog(this);
    }

    private Map<String, ERC20Token> getAvailableTokens(UUID ethAccountId) {
        Map<String, ERC20Token> supportedTokens = _mbwManager.getSupportedERC20Tokens();
        if (supportedTokens.isEmpty()) {
            return Collections.emptyMap();
        }
        WalletAccount ethAccount = _mbwManager.getWalletManager(false).getAccount(ethAccountId);
        List<String> enabledTokens = ((EthAccount) ethAccount).getEnabledTokens();
        for (String tokenName : enabledTokens) {
            supportedTokens.remove(tokenName);
        }
        return supportedTokens;
    }

    @OnClick(R.id.btEthCreate)
    void onAddEth() {
        final WalletManager wallet = _mbwManager.getWalletManager(false);
        // at this point, we have to have a master seed, since we created one on startup
        Preconditions.checkState(_mbwManager.getMasterSeedManager().hasBip32MasterSeed());

        boolean canCreateAccount = wallet.getModuleById(EthereumModule.ID).canCreateAccount(new EthereumMasterseedConfig());
        if (!canCreateAccount) {
            // TODO replace with string res
            _toaster.toast("You can only have one unused HD Ethereum Account.", false);
            return;
        }

        if (ethCreationAsyncTask == null || ethCreationAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
            boolean callERC20CreationDialog = false;
            ethCreationAsyncTask = new ETHCreationAsyncTask(callERC20CreationDialog);
            ethCreationAsyncTask.execute();
        }
    }

    @OnClick(R.id.btErc20Create)
    void onAddERC20() {
        List<WalletAccount<?>> ethAccounts = EthereumModuleKt.getEthAccounts(_mbwManager.getWalletManager(false));
        // check whether any eth account exist
        if (ethAccounts.isEmpty()) {
            // if not create eth account and call erc20 dialog after
            if (ethCreationAsyncTask == null || ethCreationAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
                boolean callERC20CreationDialog = true;
                ethCreationAsyncTask = new ETHCreationAsyncTask(callERC20CreationDialog);
                ethCreationAsyncTask.execute();
            }
        } else if (ethAccounts.size() == 1) {
            showERC20TokensOptions(ethAccounts.get(0).getId());
        } else {
            // else ask what account select for erc20 token addition
            showEthAccountsOptions();
        }
    }

    private void showEthAccountsOptions() {
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(AddAccountActivity.this, android.R.layout.select_dialog_singlechoice);
        List<WalletAccount<?>> accounts = EthereumModuleKt.getEthAccounts(_mbwManager.getWalletManager(false));
        arrayAdapter.addAll(getEthAccountsForView(accounts));
        AlertDialog.Builder dialogBuilder = getSingleChoiceDialog("Select Account", arrayAdapter);
        dialogBuilder.setPositiveButton("ok", (dialog, which) -> {
            UUID ethAccountId = accounts.get(selectedIndex).getId();
            showERC20TokensOptions(ethAccountId);
        });
        dialogBuilder.show();
    }

    private List<String> getEthAccountsForView(List<WalletAccount<?>> accounts) {
        List<String> result = new ArrayList<>();
        String denominatedValue;
        Collections.sort(accounts, (a1, a2) -> Integer.compare(((EthAccount) a1).getAccountIndex(), ((EthAccount) a2).getAccountIndex()));
        for (WalletAccount account : accounts) {
            denominatedValue = ValueExtensionsKt.toStringWithUnit(account.getAccountBalance().getSpendable(), _mbwManager.getDenomination(_mbwManager.getSelectedAccount().getCoinType()));
            result.add(account.getLabel() + " (" + denominatedValue + ")");
        }
        return result;
    }

    private void showERC20TokensOptions(UUID ethAccountId) {
        if (getAvailableTokens(ethAccountId).isEmpty()) {
            _toaster.toast("All supported tokens for this account are already added", true);
            return;
        }
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(AddAccountActivity.this, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.addAll(getAvailableTokens(ethAccountId).keySet());
        AlertDialog.Builder dialogBuilder = getSingleChoiceDialog("Select Token", arrayAdapter);
        dialogBuilder.setPositiveButton("ok", (dialog, which) -> {
            String strName = arrayAdapter.getItem(selectedIndex);
            EthAccount ethAccount = (EthAccount) _mbwManager.getWalletManager(false).getAccount(ethAccountId);
            if (ethAccount.isEnabledToken(strName)) {
                setResult(RESULT_CANCELED);
                finish();
            } else {
                new ERC20CreationAsyncTask(getAvailableTokens(ethAccountId).get(strName), ethAccountId).execute();
            }
        });
        dialogBuilder.show();
    }

    private AlertDialog.Builder getSingleChoiceDialog(String title, ArrayAdapter<String> arrayAdapter) {
        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(AddAccountActivity.this);
        dialogBuilder.setIcon(R.drawable.ic_launcher);
        dialogBuilder.setTitle(title);
        dialogBuilder.setSingleChoiceItems(arrayAdapter, selectedIndex, (dialog, which) -> selectedIndex = which);
        dialogBuilder.setNegativeButton("cancel", (dialog, which) -> dialog.dismiss());
        return dialogBuilder;
    }

    View.OnClickListener advancedClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
                @Override
                public void run() {
                    AddAdvancedAccountActivity.callMe(AddAccountActivity.this, ADD_ADVANCED_CODE);
                }
            });
        }

    };

    View.OnClickListener createHdAccount = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
                @Override
                public void run() {
                    createNewHdAccount();
                }
            });
        }
    };

    View.OnClickListener createColuAccount = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = AddColuAccountActivity.getIntent(AddAccountActivity.this);
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            AddAccountActivity.this.startActivity(intent);
            AddAccountActivity.this.finish();
        }
    };

    private void createNewHdAccount() {
        final WalletManager wallet = _mbwManager.getWalletManager(false);
        // at this point, we have to have a master seed, since we created one on startup
        Preconditions.checkState(_mbwManager.getMasterSeedManager().hasBip32MasterSeed());

        boolean canCreateAccount = wallet.getModuleById(BitcoinHDModule.ID).canCreateAccount(new AdditionalHDAccountConfig());
        if (!canCreateAccount) {
            _toaster.toast(R.string.use_acc_first, false);
            return;
        }
        showProgress(R.string.hd_account_creation_started);
        new HdCreationAsyncTask().execute();
    }

    private class HdCreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
        @Override
        protected UUID doInBackground(Void... params) {
            return _mbwManager.getWalletManager(false).createAccounts(new AdditionalHDAccountConfig()).get(0);
        }

        @Override
        protected void onPostExecute(UUID account) {
            _progress.dismiss();
            MbwManager.getEventBus().post(new AccountCreated(account));
            MbwManager.getEventBus().post(new AccountChanged(account));
            finishOk(account);
        }
    }

    private class ETHCreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
        boolean callERC20CreationDialog;

        ETHCreationAsyncTask(boolean callERC20CreationDialog) {
            this.callERC20CreationDialog = callERC20CreationDialog;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(R.string.eth_account_creation_started);
        }

        @Override
        protected UUID doInBackground(Void... params) {
            List<UUID> accounts = _mbwManager.getWalletManager(false).createAccounts(new EthereumMasterseedConfig());
            return accounts.get(0);
        }

        @Override
        protected void onPostExecute(UUID account) {
            _progress.dismiss();
            MbwManager.getEventBus().post(new AccountCreated(account));
            MbwManager.getEventBus().post(new AccountChanged(account));
            if (callERC20CreationDialog) {
                showERC20TokensOptions(account);
            } else {
                finishOk(account);
            }
        }
    }

    private class ERC20CreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
        ERC20Token token;
        UUID ethAccountId;

        ERC20CreationAsyncTask(ERC20Token token, UUID ethAccountId) {
            this.token = token;
            this.ethAccountId = ethAccountId;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(R.string.erc20_account_creation_started);
        }

        @Override
        protected UUID doInBackground(Void... params) {
            List<UUID> accounts = _mbwManager.getWalletManager(false).createAccounts(new ERC20Config(token, ethAccountId));
            return accounts.get(0);
        }

        @Override
        protected void onPostExecute(UUID account) {
            _progress.dismiss();
            MbwManager.getEventBus().post(new AccountCreated(account));
            MbwManager.getEventBus().post(new AccountChanged(account));
            EthAccount ethAccount = (EthAccount) _mbwManager.getWalletManager(false).getAccount(ethAccountId);
            ethAccount.addEnabledToken(token.getName());
            finishOk(account);
        }
    }

    private void showProgress(@StringRes int res) {
        _progress.setCancelable(false);
        _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        _progress.setMessage(getString(res));
        _progress.show();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == ADD_ADVANCED_CODE) {
            if (resultCode == RESULT_CANCELED) {
                //stay here
                return;
            }
            //just pass on what we got
            setResult(resultCode, intent);
            finish();
        } else if (requestCode == IMPORT_SEED_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                UUID account = (UUID) intent.getSerializableExtra(RESULT_KEY);
                finishOk(account);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void finishOk(UUID account) {
        Intent result = new Intent();
        result.putExtra(RESULT_KEY, account);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onResume() {
        MbwManager.getEventBus().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        _progress.dismiss();
        MbwManager.getEventBus().unregister(this);
        _mbwManager.getVersionManager().closeDialog();
        super.onPause();
    }
}
