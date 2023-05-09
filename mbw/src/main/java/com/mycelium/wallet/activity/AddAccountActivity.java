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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.common.base.Preconditions;
import com.mycelium.bequant.BequantConstants;
import com.mycelium.bequant.BequantPreference;
import com.mycelium.bequant.intro.BequantIntroActivity;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.addaccount.ERC20CreationAsyncTask;
import com.mycelium.wallet.activity.addaccount.ERC20EthAccountAdapter;
import com.mycelium.wallet.activity.addaccount.ERC20TokenAdapter;
import com.mycelium.wallet.activity.addaccount.ETHCreationAsyncTask;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.settings.SettingsPreference;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.databinding.AddAccountActivityBinding;
import com.mycelium.wallet.databinding.LayoutSelectEthAccountToErc20Binding;
import com.mycelium.wallet.databinding.LayoutSelectEthAccountToErc20TitleBinding;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.AccountCreated;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.btcvault.hd.AdditionalMasterseedAccountConfig;
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHDModule;
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token;
import com.mycelium.wapi.wallet.eth.EthAccount;
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig;
import com.mycelium.wapi.wallet.eth.EthereumModule;
import com.mycelium.wapi.wallet.eth.EthereumModuleKt;
import com.mycelium.wapi.wallet.fio.FIOMasterseedConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class AddAccountActivity extends AppCompatActivity {
    private AddAccountActivityBinding binding;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        binding = AddAccountActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        _mbwManager = MbwManager.getInstance(this);
        _toaster = new Toaster(this);

        binding.btAdvanced.setOnClickListener(advancedClickListener);
        binding.btHdCreate.setOnClickListener(createHdAccount);
        binding.btFIOCreate.setOnClickListener(createFioAccount);
        if (_mbwManager.getMetadataStorage().getMasterSeedBackupState() == MetadataStorage.BackupState.VERIFIED) {
            binding.tvWarningNoBackup.setVisibility(View.GONE);
        } else {
            binding.tvInfoBackup.setVisibility(View.GONE);
        }
        _progress = new ProgressDialog(this);
        binding.btInvestmentCreate.setOnClickListener(v ->
                startActivity(new Intent(AddAccountActivity.this, BequantIntroActivity.class)));
        binding.btBTCVHDCreate.setOnClickListener(v -> _mbwManager.runPinProtectedFunction(this, () -> {
            final WalletManager wallet = _mbwManager.getWalletManager(false);
            // at this point, we have to have a master seed, since we created one on startup
            Preconditions.checkState(_mbwManager.getMasterSeedManager().hasBip32MasterSeed());

            boolean canCreateAccount = wallet.getModuleById(BitcoinVaultHDModule.ID)
                    .canCreateAccount(new AdditionalMasterseedAccountConfig());
            if (!canCreateAccount) {
                // TODO replace with string res
                _toaster.toast("You can only have one unused Bitcoin Vault Account.", false);
                return;
            }

            new BTCVHdCreationAsyncTask().execute();
        }));
        binding.btEthCreate.setOnClickListener(v -> {
            _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
                @Override
                public void run() {
                    final WalletManager wallet = _mbwManager.getWalletManager(false);
                    // at this point, we have to have a master seed, since we created one on startup
                    Preconditions.checkState(_mbwManager.getMasterSeedManager().hasBip32MasterSeed());

                    boolean canCreateAccount = wallet.getModuleById(EthereumModule.ID).canCreateAccount(new EthereumMasterseedConfig());
                    if (!canCreateAccount) {
                        // TODO replace with string res
                        _toaster.toast("You can only have one unused Ethereum Account.", false);
                        return;
                    }

                    if (ethCreationAsyncTask == null || ethCreationAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
                        ethCreationAsyncTask = new ETHCreationAsyncTask(_mbwManager,
                                () -> {
                                    showProgress(R.string.eth_account_creation_started);
                                    return null;
                                },
                                uuid -> {
                                    _progress.dismiss();
                                    finishOk(uuid);
                                    return null;
                                });
                        ethCreationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            });
        });
        binding.btErc20Create.setOnClickListener(v -> showEthAccountsOptions());
    }

    /**
     * @return all supported tokens or all supported tokens except these that are already enabled for
     * account with ethAccountId.
     */
    private List<ERC20Token> getAddedTokens(@Nullable UUID ethAccountId) {
        List<ERC20Token> result = new ArrayList<>();
        Map<String, ERC20Token> supportedTokens = _mbwManager.getSupportedERC20Tokens();
        if (ethAccountId != null) {
            WalletAccount ethAccount = _mbwManager.getWalletManager(false).getAccount(ethAccountId);
            List<String> enabledTokens = ((EthAccount) ethAccount).getEnabledTokens();
            for (Map.Entry<String, ERC20Token> entry : supportedTokens.entrySet()) {
                if (enabledTokens.contains(entry.getKey())) {
                    result.add(entry.getValue());
                }
            }
        }
        return result;
    }

    private void showEthAccountsOptions() {
        final ERC20EthAccountAdapter arrayAdapter = new ERC20EthAccountAdapter(this, R.layout.checked_item);
        List<WalletAccount<?>> accounts = EthereumModuleKt.getActiveEthAccounts(_mbwManager.getWalletManager(false));
        arrayAdapter.addAll(getEthAccountsForView(accounts));
        arrayAdapter.add(getString(R.string.create_new_account));
        LayoutSelectEthAccountToErc20Binding selectBinding = LayoutSelectEthAccountToErc20Binding.inflate(LayoutInflater.from(this));
        selectBinding.list.setAdapter(arrayAdapter);
        new AlertDialog.Builder(this, R.style.MyceliumModern_Dialog_BlueButtons)
                .setCustomTitle(LayoutInflater.from(this).inflate(R.layout.layout_select_eth_account_to_erc20_title, null))
                .setView(selectBinding.getRoot())
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    int selected = arrayAdapter.getSelected();
                    if (selected == arrayAdapter.getCount() - 1) {
                        // "Create new account" item
                        showERC20TokensOptions(null);
                    } else {
                        UUID ethAccountId = accounts.get(selected).getId();
                        showERC20TokensOptions(ethAccountId);
                    }
                })
                .show();
    }

    private List<String> getEthAccountsForView(List<WalletAccount<?>> accounts) {
        List<String> result = new ArrayList<>();
        Collections.sort(accounts, (a1, a2) -> Integer.compare(((EthAccount) a1).getAccountIndex(), ((EthAccount) a2).getAccountIndex()));
        for (WalletAccount account : accounts) {
            String denominatedValue = ValueExtensionsKt.toStringWithUnit(
                    account.getAccountBalance().getSpendable(), _mbwManager.getDenomination(account.getCoinType()));
            result.add(account.getLabel() + " (" + denominatedValue + ")");
        }
        return result;
    }

    /**
     * After selecting the desired erc20 token depending on whether ethAccountId is null or not
     * we create or don't create new ethereum account before creating erc20 account.
     *
     * @param ethAccountId if it's null we would need to create new ethereum account first
     * before creating erc20 account
     */
    Button positiveButton = null;

    private void showERC20TokensOptions(@Nullable UUID ethAccountId) {
        final EthAccount ethAccount = ethAccountId != null ? (EthAccount) _mbwManager.getWalletManager(false).getAccount(ethAccountId) : null;
        List<ERC20Token> supportedTokens = new ArrayList<>(_mbwManager.getSupportedERC20Tokens().values());
        Collections.sort(supportedTokens, (a1, a2) -> a1.getName().compareTo(a2.getName()));
        List<ERC20Token> addedTokens = getAddedTokens(ethAccountId);
        final ERC20TokenAdapter arrayAdapter = new ERC20TokenAdapter(AddAccountActivity.this,
                R.layout.token_item,
                supportedTokens,
                addedTokens);

        arrayAdapter.setSelectListener(new Function1<List<ERC20Token>, Unit>() {
            @Override
            public Unit invoke(List<ERC20Token> erc20Tokens) {
                positiveButton.setEnabled(!erc20Tokens.isEmpty());
                return null;
            }
        });
        LayoutSelectEthAccountToErc20TitleBinding titleBinding = LayoutSelectEthAccountToErc20TitleBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyceliumModern_Dialog_BlueButtons)
                .setAdapter(arrayAdapter, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        if (addedTokens.size() < supportedTokens.size()) {
            titleBinding.titleText.setText(ethAccount != null ?
                    getString(R.string.select_token, ethAccount.getLabel()) : getString(R.string.select_token_new_account));
            builder.setPositiveButton(R.string.ok, (dialog, which) -> {
                if (ethAccount != null) {
                    new ERC20CreationAsyncTask(_mbwManager, arrayAdapter.getSelectedList(), ethAccount,
                            () -> {
                                showProgress(R.string.erc20_account_creation_started);
                                return null;
                            },
                            uuids -> {
                                _progress.dismiss();
                                if (uuids.isEmpty()) {
                                    _toaster.toast("Error. No account created!", false);
                                    setResult(Activity.RESULT_CANCELED);
                                    finish();
                                } else {
                                    finishOk(uuids.get(0));
                                }
                                return null;
                            }
                    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    // we need new ethereum account, so create it first and erc20 account after. pass which token we want to create then
                    if (ethCreationAsyncTask == null || ethCreationAsyncTask.getStatus() != AsyncTask.Status.RUNNING) {
                        List<ERC20Token> list = arrayAdapter.getSelectedList();
                        ethCreationAsyncTask = new ETHCreationAsyncTask(_mbwManager,
                                () -> {
                                    showProgress(R.string.eth_account_creation_started);
                                    return null;
                                },
                                uuid -> {
                                    EthAccount ethAccount1 = (EthAccount) _mbwManager.getWalletManager(false).getAccount(uuid);
                                    new ERC20CreationAsyncTask(_mbwManager, list, ethAccount1,
                                            () -> {
                                                showProgress(R.string.erc20_account_creation_started);
                                                return null;
                                            },
                                            uuids -> {
                                                if (uuids.isEmpty()) {
                                                    _toaster.toast("Error. No account created!", false);
                                                    setResult(RESULT_CANCELED);
                                                    finish();
                                                } else {
                                                    finishOk(uuids.get(0));
                                                }
                                                return null;
                                            }
                                    ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                    return null;
                                });
                        ethCreationAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            });
        } else {
            titleBinding.titleText.setText(ethAccount != null ?
                    getString(R.string.list_added_tokens, ethAccount.getLabel()) : getString(R.string.list_added_tokens_new_account));
        }
        builder.setCustomTitle(titleBinding.getRoot());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setEnabled(false);
            }
        });
        dialog.show();
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

    View.OnClickListener createFioAccount = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            _mbwManager.runPinProtectedFunction(AddAccountActivity.this, new Runnable() {
                @Override
                public void run() {
                    createFIOAccount();
                }
            });
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

    private void createFIOAccount() {
        showProgress(R.string.fio_account_creation_started);
        new FIOCreationAsyncTask().execute();
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

    private class BTCVHdCreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(R.string.btcv_account_creation_started);
        }

        @Override
        protected UUID doInBackground(Void... params) {
            return _mbwManager.getWalletManager(false).createAccounts(new AdditionalMasterseedAccountConfig()).get(0);
        }

        @Override
        protected void onPostExecute(UUID account) {
            _progress.dismiss();
            MbwManager.getEventBus().post(new AccountCreated(account));
            MbwManager.getEventBus().post(new AccountChanged(account));
            finishOk(account);
        }
    }

    private class FIOCreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
        @Override
        protected UUID doInBackground(Void... params) {
            return _mbwManager.getWalletManager(false).createAccounts(new FIOMasterseedConfig()).get(0);
        }

        @Override
        protected void onPostExecute(UUID account) {
            _progress.dismiss();
            MbwManager.getEventBus().post(new AccountCreated(account));
            MbwManager.getEventBus().post(new AccountChanged(account));
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

        binding.btInvestmentCreate.setVisibility(
                BequantPreference.isLogged() || !SettingsPreference.isContentEnabled(BequantConstants.PARTNER_ID) ?
                        View.GONE : View.VISIBLE);
    }

    @Override
    public void onPause() {
        _progress.dismiss();
        MbwManager.getEventBus().unregister(this);
        _mbwManager.getVersionManager().closeDialog();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
