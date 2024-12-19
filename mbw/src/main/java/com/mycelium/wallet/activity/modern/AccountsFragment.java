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

package com.mycelium.wallet.activity.modern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.mycelium.wallet.activity.settings.HelpFragmentKt.boostGapLimitDialog;
import static com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModuleKt.getActiveMasterseedAccounts;
import static com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModuleKt.getActiveMasterseedHDAccounts;
import static com.mycelium.wapi.wallet.colu.ColuModuleKt.getColuAccounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ActionMode.Callback;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.Lists;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mycelium.bequant.intro.BequantIntroActivity;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.AddAccountActivity;
import com.mycelium.wallet.activity.AddAdvancedAccountActivity;
import com.mycelium.wallet.activity.MessageSigningActivity;
import com.mycelium.wallet.activity.export.ShamirSharingActivity;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.fio.AboutFIOProtocolDialog;
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity;
import com.mycelium.wallet.activity.modern.adapter.AccountListAdapter;
import com.mycelium.wallet.activity.modern.event.SelectTab;
import com.mycelium.wallet.activity.modern.helper.FioHelper;
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.activity.view.DividerItemDecoration;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.AccountListChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.ExchangeSourceChanged;
import com.mycelium.wallet.event.ExtraAccountsChanged;
import com.mycelium.wallet.event.ReceivingAddressChanged;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncFailed;
import com.mycelium.wallet.event.SyncProgressUpdated;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.CreateTrader;
import com.mycelium.wallet.lt.api.DeleteTrader;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.AddressColuConfig;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.colu.ColuAccountContext;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.erc20.ERC20Account;
import com.mycelium.wapi.wallet.erc20.ERC20ModuleKt;
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account;
import com.mycelium.wapi.wallet.eth.EthAccount;
import com.mycelium.wapi.wallet.eth.EthereumModuleKt;
import com.mycelium.wapi.wallet.fio.FioAccount;
import com.mycelium.wapi.wallet.fio.FioModule;
import com.mycelium.wapi.wallet.fio.FioModuleKt;
import com.mycelium.wapi.wallet.fio.RegisteredFIOName;
import com.mycelium.wapi.wallet.manager.Config;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AccountsFragment extends Fragment {
    public static final int ADD_RECORD_RESULT_CODE = 0;

    public static final String TAG = "AccountsFragment";

    private WalletManager walletManager;

    private MetadataStorage _storage;
    private MbwManager _mbwManager;
    private LocalTraderManager localTraderManager;
    private Toaster _toaster;
    private ProgressDialog _progress;
    private RecyclerView rvRecords;
    private View llLocked;
    private AccountListAdapter accountListAdapter;
    private View root;
    private Bus eventBus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root == null) {
            root = inflater.inflate(R.layout.fragment_accounts, container, false);
        }
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (rvRecords == null) {
            rvRecords = view.findViewById(R.id.rvRecords);
            accountListAdapter = new AccountListAdapter(this, _mbwManager);
            rvRecords.setAdapter(accountListAdapter);
            rvRecords.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.divider_account_list)
                    , LinearLayoutManager.VERTICAL));
            rvRecords.setHasFixedSize(true);
            if (rvRecords.getItemAnimator() != null) {
                rvRecords.getItemAnimator().setChangeDuration(0); //avoid item blinking
            }
        }
        if (llLocked == null) {
            llLocked = view.findViewById(R.id.llLocked);
        }
        accountListAdapter.setItemClickListener(recordAddressClickListener);
        accountListAdapter.setInvestmentAccountClickListener(new AccountListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(@NotNull WalletAccount<? extends Address> account) {
                startActivity(new Intent(requireContext(), BequantIntroActivity.class));
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        _mbwManager = MbwManager.getInstance(context);
        walletManager = _mbwManager.getWalletManager(false);
        localTraderManager = _mbwManager.getLocalTraderManager();
        localTraderManager.subscribe(ltSubscriber);
        _storage = _mbwManager.getMetadataStorage();
        eventBus = MbwManager.getEventBus();
        _toaster = new Toaster(this);
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        eventBus.register(this);
        getView().findViewById(R.id.btUnlock).setOnClickListener(unlockClickedListener);
        update();
        _progress = new ProgressDialog(getActivity());
        super.onResume();
    }

    @Override
    public void onPause() {
        _progress.dismiss();
        eventBus.unregister(this);
        super.onPause();
    }

    @Override
    public void onDetach() {
        localTraderManager.unsubscribe(ltSubscriber);
        super.onDetach();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser) {
            finishCurrentActionMode();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        requireActivity().invalidateOptionsMenu();
        if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            UUID accountid = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
            if (accountid != null) {
                //check whether the account is active - we might have scanned the priv key for an archived watchonly
                WalletAccount account = walletManager.getAccount(accountid);
                if (account.isActive()) {
                    _mbwManager.setSelectedAccount(accountid);
                }
                accountListAdapter.setFocusedAccountId(account.getId());
                updateIncludingMenus();
                if (!(account instanceof ColuAccount) && !(account instanceof ERC20Account)
                        && !intent.getBooleanExtra(AddAccountActivity.IS_UPGRADE, false)) {
                    setLabelOnAccount(account, account.getLabel(), false);
                }
                eventBus.post(new ExtraAccountsChanged());
                eventBus.post(new AccountChanged(accountid));
            }
        } else if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == AddAdvancedAccountActivity.RESULT_MSG) {
            new AlertDialog.Builder(getActivity())
                    .setMessage(intent.getStringExtra(AddAccountActivity.RESULT_MSG))
                    .setPositiveButton(R.string.button_ok, null)
                    .create()
                    .show();
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void interruptSync(Collection<WalletAccount> accountsToInterrupt) {
        for (WalletAccount<?> wa : accountsToInterrupt) {
            wa.interruptSync();
        }
    }

    private void deleteAccount(final WalletAccount accountToDelete) {
        checkNotNull(accountToDelete);
        final List<WalletAccount> linkedAccounts = new ArrayList<>();
        if (accountToDelete instanceof EthAccount) {
            linkedAccounts.addAll(getLinkedERC20Accounts(accountToDelete));
        } else if (accountToDelete instanceof ERC20Account) {
            linkedAccounts.add(getLinkedEthAccount(accountToDelete));
        } else {
            if (getLinkedAccount(accountToDelete) != null) {
                linkedAccounts.add(getLinkedAccount(accountToDelete));
            }
        }

        Collection<WalletAccount> accountsToInterrupt = new HashSet<>();
        accountsToInterrupt.add(accountToDelete);
        accountsToInterrupt.addAll(linkedAccounts);
        interruptSync(accountsToInterrupt);

        final View checkBoxView = View.inflate(getActivity(), R.layout.delkey_checkbox, null);
        final CheckBox keepAddrCheckbox = checkBoxView.findViewById(R.id.checkbox);
        keepAddrCheckbox.setText(getString(R.string.keep_account_address));
        keepAddrCheckbox.setChecked(false);

        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getActivity());
        deleteDialog.setTitle(R.string.delete_account_title);
        deleteDialog.setMessage(Html.fromHtml(createDeleteDialogText(accountToDelete, linkedAccounts)));

        // add checkbox only for SingleAddressAccounts and only if a private key is present
        final boolean hasPrivateData = (accountToDelete instanceof ExportableAccount
                && ((ExportableAccount) accountToDelete).getExportData(AesKeyCipher.defaultKeyCipher()).getPrivateData().isPresent());

        if (accountToDelete instanceof SingleAddressAccount && hasPrivateData) {
            deleteDialog.setView(checkBoxView);
        }

        if (accountToDelete instanceof ColuAccount && accountToDelete.canSpend()) {
            Log.d(TAG, "Preparing to delete a colu account.");
            deleteDialog.setView(checkBoxView);
        }

        deleteDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                Log.d(TAG, "Entering onClick delete");
                if (accountToDelete.getId().equals(localTraderManager.getLocalTraderAccountId())) {
                    localTraderManager.unsetLocalTraderAccount();
                }
                if (hasPrivateData) {
                    Value potentialBalance = getPotentialBalance(accountToDelete);
                    AlertDialog.Builder confirmDeleteDialog = new AlertDialog.Builder(getActivity());
                    confirmDeleteDialog.setTitle(R.string.confirm_delete_pk_title);

                    // Set the message. There are four combinations, with and without label, with and without BTC amount.
                    String label = _mbwManager.getMetadataStorage().getLabelByAccount(accountToDelete.getId());
                    int labelCount = 1;
                    if (!linkedAccounts.isEmpty()) {
                        label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccounts.get(0).getId());
                        labelCount++;
                    }
                    String message;

                    // For active accounts we check whether there is money on them before deleting. we don't know if there
                    // is money on archived accounts
                    String address;
                    if (accountToDelete instanceof SingleAddressAccount) {
                        Map<AddressType, BitcoinAddress> addressMap = ((SingleAddressAccount) accountToDelete).getPublicKey().
                                getAllSupportedAddresses(_mbwManager.getNetwork());
                        address = TextUtils.join("\n\n", addressMap.values());
                    } else {
                        Address receivingAddress = accountToDelete.getReceiveAddress();
                        if (receivingAddress != null) {
                            address = AddressUtils.toMultiLineString(receivingAddress.toString());
                        } else {
                            address = "";
                        }
                    }
                    if (accountToDelete.isActive() && potentialBalance != null && potentialBalance.moreThanZero()) {
                        if (label.length() != 0) {
                            message = getResources().getQuantityString(R.plurals.confirm_delete_pk_with_balance_with_label,
                                    !(accountToDelete instanceof SingleAddressAccount) ? 1 : 0,
                                    getResources().getQuantityString(R.plurals.account_label, labelCount, label),
                                    address, getBalanceString(accountToDelete.getCoinType(), accountToDelete.getAccountBalance()));
                        } else {
                            message = getResources().getQuantityString(R.plurals.confirm_delete_pk_with_balance,
                                    !(accountToDelete instanceof SingleAddressAccount) ? 1 : 0,
                                    getBalanceString(accountToDelete.getCoinType(), accountToDelete.getAccountBalance()));
                        }
                    } else {
                        if (label.length() != 0) {
                            message = getResources().getQuantityString(R.plurals.confirm_delete_pk_without_balance_with_label,
                                    !(accountToDelete instanceof SingleAddressAccount) ? 1 : 0,
                                    getResources().getQuantityString(R.plurals.account_label, labelCount, label), address);
                        } else {
                            message = getResources().getQuantityString(R.plurals.confirm_delete_pk_without_balance,
                                    !(accountToDelete instanceof SingleAddressAccount) ? 1 : 0, address);
                        }
                    }
                    confirmDeleteDialog.setMessage(message);

                    confirmDeleteDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            Log.d(TAG, "In deleteFragment onClick");
                            if (keepAddrCheckbox.isChecked() && accountToDelete instanceof SingleAddressAccount) {
                                try {
                                    //Check if this SingleAddress account is related with ColuAccount
                                    WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, walletManager.getAccounts());
                                    if (linkedColuAccount instanceof ColuAccount) {
                                        walletManager.deleteAccount(linkedColuAccount.getId());
                                        walletManager.deleteAccount(accountToDelete.getId());
                                        ColuAccountContext context = ((ColuAccount) linkedColuAccount).getContext();
                                        ColuMain coluMain = (ColuMain) linkedColuAccount.getCoinType();
                                        Config config = new AddressColuConfig(context.getAddress().get(AddressType.P2PKH), coluMain);
                                        _storage.deleteAccountMetadata(linkedColuAccount.getId());
                                        walletManager.createAccounts(config);
                                    } else {
                                        ((SingleAddressAccount) accountToDelete).forgetPrivateKey(AesKeyCipher.defaultKeyCipher());
                                    }
                                    _toaster.toast(R.string.private_key_deleted, false);
                                } catch (KeyCipher.InvalidKeyCipher e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                if (accountToDelete instanceof ColuAccount) {
                                    try {
                                        walletManager.deleteAccount(accountToDelete.getId());
                                        WalletAccount linkedAccount = Utils.getLinkedAccount(accountToDelete, walletManager.getAccounts());
                                        if (linkedAccount != null) {
                                            walletManager.deleteAccount(linkedAccount.getId());
                                            _storage.deleteAccountMetadata(linkedAccount.getId());
                                        }
                                        if (keepAddrCheckbox.isChecked()) {
                                            ColuAccountContext context = ((ColuAccount) accountToDelete).getContext();
                                            ColuMain coluMain = (ColuMain) accountToDelete.getCoinType();
                                            Config config = new AddressColuConfig(context.getAddress().get(AddressType.P2PKH), coluMain);
                                            _storage.deleteAccountMetadata(accountToDelete.getId());
                                            walletManager.createAccounts(config);
                                        } else {
                                            _storage.deleteAccountMetadata(accountToDelete.getId());
                                            _toaster.toast("Deleting account.", false);
                                            _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveSpendingAccounts().get(0).getId());
                                        }
                                    } catch (Exception e) {
                                        // make a message !
                                        Log.e(TAG, getString(R.string.colu_error_deleting), e);
                                        _toaster.toast(getString(R.string.colu_error_deleting), false);
                                    }
                                } else {
                                    //Check if this SingleAddress account is related with ColuAccount
                                    WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, walletManager.getAccounts());
                                    if (linkedColuAccount instanceof ColuAccount) {
                                        walletManager.deleteAccount(linkedColuAccount.getId());
                                        _storage.deleteAccountMetadata(linkedColuAccount.getId());
                                    }
                                    walletManager.deleteAccount(accountToDelete.getId());
                                    _storage.deleteAccountMetadata(accountToDelete.getId());
                                    _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveSpendingAccounts().get(0).getId());
                                    _toaster.toast(R.string.account_deleted, false);
                                }
                            }
                            finishCurrentActionMode();
                            eventBus.post(new AccountChanged(accountToDelete.getId()));
                        }
                    });
                    confirmDeleteDialog.setNegativeButton(R.string.no, null);
                    confirmDeleteDialog.show();
                } else {
                    // account has no private data - dont make a fuzz about it and just delete it
                    walletManager.deleteAccount(accountToDelete.getId());
                    _storage.deleteAccountMetadata(accountToDelete.getId());
                    // remove linked accounts if necessary
                    if (accountToDelete instanceof EthAccount) {
                        for (WalletAccount walletAccount : getLinkedERC20Accounts(accountToDelete)) {
                            walletManager.deleteAccount(walletAccount.getId());
                            _storage.deleteAccountMetadata(walletAccount.getId());
                        }
                    } else if (accountToDelete instanceof ERC20Account) {
                        EthAccount ethAccount = getLinkedEthAccount(accountToDelete);
                        ethAccount.updateEnabledTokens();
                    } else {
                        //Check if this SingleAddress account is related with ColuAccount
                        WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, walletManager.getAccounts());
                        if (linkedColuAccount != null) {
                            walletManager.deleteAccount(linkedColuAccount.getId());
                            _storage.deleteAccountMetadata(linkedColuAccount.getId());
                        }
                    }
                    finishCurrentActionMode();
                    eventBus.post(new AccountChanged(accountToDelete.getId()));
                    _toaster.toast(R.string.account_deleted, false);
                }
            }

            private Value getPotentialBalance(WalletAccount account) {
                if (account.isArchived()) {
                    return null;
                } else {
                    return account.getAccountBalance().getSpendable();
                }
            }
        });
        deleteDialog.setNegativeButton(R.string.no, null).show();
    }

    private EthAccount getLinkedEthAccount(WalletAccount account) {
        return (EthAccount) Utils.getLinkedAccount(account, EthereumModuleKt.getEthAccounts(walletManager));
    }

    private List<WalletAccount> getLinkedERC20Accounts(WalletAccount account) {
        return Utils.getLinkedAccounts(account, ERC20ModuleKt.getERC20Accounts(walletManager));
    }

    private List<WalletAccount> getActiveLinkedERC20Accounts(WalletAccount account) {
        return Utils.getLinkedAccounts(account, ERC20ModuleKt.getActiveERC20Accounts(walletManager));
    }

    @NonNull
    private String createDeleteDialogText(WalletAccount accountToDelete, List<WalletAccount> linkedAccounts) {
        String accountName = _mbwManager.getMetadataStorage().getLabelByAccount(accountToDelete.getId());
        String dialogText;

        if (accountToDelete.isActive()) {
            dialogText = getActiveAccountDeleteText(accountToDelete, linkedAccounts, accountName);
        } else {
            dialogText = getArchivedAccountDeleteText(linkedAccounts, accountName);
        }
        return dialogText;
    }

    @NonNull
    private String getArchivedAccountDeleteText(List<WalletAccount> linkedAccounts, String accountName) {
        String dialogText;
        if (linkedAccounts.size() > 1) {
            dialogText = getString(R.string.delete_archived_account_message_s, accountName);
        } else if (!linkedAccounts.isEmpty() && linkedAccounts.get(0).isVisible()) {
            String linkedAccountName = _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccounts.get(0).getId());
            dialogText = getString(R.string.delete_archived_account_message, accountName, linkedAccountName);
        } else {
            dialogText = getString(R.string.delete_archived_account_message_s, accountName);
        }
        return dialogText;
    }

    @NonNull
    private String getActiveAccountDeleteText(WalletAccount accountToDelete, List<WalletAccount> linkedAccounts, String accountName) {
        String dialogText;
        Balance balance = checkNotNull(accountToDelete.getAccountBalance());
        String valueString = getBalanceString(accountToDelete.getCoinType(), balance);

        // TODO sort linkedAccounts for visible only
        if (linkedAccounts.size() > 1 || accountToDelete instanceof EthAccount && linkedAccounts.size() > 0) {
            List<String> linkedAccountStrings = new ArrayList<>();
            for (WalletAccount linkedAccount : linkedAccounts) {
                Balance linkedBalance = linkedAccount.getAccountBalance();
                String linkedAccountName = _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
                String linkedValueString = getBalanceString(linkedAccount.getCoinType(), linkedBalance);
                linkedAccountStrings.add("<b>" + linkedAccountName + "</b> holding <b>" + linkedValueString + "</b>");
            }
            String linkedAccountsString = TextUtils.join(", ", linkedAccountStrings) + "?";
            dialogText = getString(R.string.delete_accounts_message, accountName, valueString,
                    linkedAccountsString) + "\n" + getString(R.string.both_eth_and_tokens_will_deleted, accountName);
        } else if (!linkedAccounts.isEmpty() && linkedAccounts.get(0).isVisible() && !(accountToDelete instanceof ERC20Account)) {
            Balance linkedBalance = linkedAccounts.get(0).getAccountBalance();
            String linkedValueString = getBalanceString(linkedAccounts.get(0).getCoinType(), linkedBalance);
            String linkedAccountName = _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccounts.get(0).getId());
            dialogText = getString(R.string.delete_account_message, accountName, valueString,
                    linkedAccountName, linkedValueString) + "\n" +
                    getString(R.string.both_rmc_will_deleted, accountName, linkedAccountName);
        } else {
            dialogText = getString(R.string.delete_account_message_s, accountName, valueString);
        }
        return dialogText;
    }

    private String getBalanceString(AssetInfo coinType, Balance balance) {
        return ValueExtensionsKt.toStringWithUnit(balance.getSpendable(), _mbwManager.getDenomination(coinType));
    }

    /**
     * If account is colu we are asking for linked BTC. Else we are searching if any colu attached.
     */
    private WalletAccount getLinkedAccount(WalletAccount account) {
        return Utils.getLinkedAccount(account, walletManager.getAccounts());
    }

    private void finishCurrentActionMode() {
        if (currentActionMode != null) {
            currentActionMode.finish();
        }
    }

    private void update() {
        if (!isAdded()) {
            return;
        }

        if (_mbwManager.isKeyManagementLocked()) {
            // Key management is locked
            rvRecords.setVisibility(View.GONE);
            llLocked.setVisibility(View.VISIBLE);
        } else {
            // Make all the key management functionality available to experts
            rvRecords.setVisibility(View.VISIBLE);
            llLocked.setVisibility(View.GONE);
        }
        eventBus.post(new AccountListChanged());
    }

    private ActionMode currentActionMode;

    private AccountListAdapter.ItemClickListener recordAddressClickListener = new AccountListAdapter.ItemClickListener() {
        @Override
        public void onItemClick(@NonNull WalletAccount account) {
            // Check whether a new account was selected
            if (!_mbwManager.getSelectedAccount().equals(account) && account.isActive()) {
                _mbwManager.setSelectedAccount(account.getId());
            }
            toastSelectedAccountChanged(account);
            updateIncludingMenus();
        }
    };

    private void updateIncludingMenus() {
        WalletAccount account = requireFocusedAccount();
        boolean isBch = account instanceof SingleAddressBCHAccount
                || account instanceof Bip44BCHAccount;

        final List<Integer> menus = Lists.newArrayList();
        if (!(account instanceof ColuAccount)
                && !Utils.checkIsLinked(account, getColuAccounts(walletManager))) {
            menus.add(R.menu.record_options_menu);
        }

        final List<RegisteredFIOName> fioNames = ((FioModule) walletManager.getModuleById(FioModule.ID)).getAllRegisteredFioNames();
        if (account instanceof FioAccount) {
            if (account.canSpend()) {
                menus.add(R.menu.record_options_menu_add_fio_name);
            }
            if (fioNames.isEmpty()) {
                menus.add(R.menu.record_options_menu_about_fio_protocol);
            } else {
                menus.add(R.menu.record_options_menu_my_fio_names);
                menus.add(R.menu.record_options_menu_about_fio_protocol);
                menus.add(R.menu.record_options_menu_fio_requests);
            }
        }

        if (account instanceof SingleAddressAccount ||
                (account.isDerivedFromInternalMasterseed() && !(account instanceof FioAccount))) {
            menus.add(R.menu.record_options_menu_backup);
        }

        if (account instanceof SingleAddressAccount) {
            menus.add(R.menu.record_options_menu_backup_verify);
            menus.add(R.menu.record_options_menu_shamir);
        }

        if (account instanceof ColuAccount) {
            //TODO: distinguish between ColuAccount in single address mode and HD mode
            menus.add(R.menu.record_options_menu_backup);
            menus.add(R.menu.record_options_menu_backup_verify);
        }

        if (_mbwManager.isAccountCanBeDeleted(account)) {
            menus.add(R.menu.record_options_menu_delete);
        }

        if (account.isActive() && account.canSpend() && !isBch && account.canSign()) {
            menus.add(R.menu.record_options_menu_sign);
        }

        if (account.isActive() && !isBch) {
            menus.add(R.menu.record_options_menu_active);
        }

        if (account.isActive() && !isBch && !(account instanceof AbstractEthERC20Account)
                && !(account instanceof FioAccount)) {
            menus.add(R.menu.record_options_menu_outputs);
        }

        if (!(account instanceof Bip44BCHAccount)
                && !(account instanceof SingleAddressBCHAccount)
                && account.isArchived()) {
            menus.add(R.menu.record_options_menu_archive);
        }

        if (account.isActive() && account instanceof ExportableAccount && !isBch) {
            menus.add(R.menu.record_options_menu_export);
        }

        final List<FioAccount> fioAccounts = FioModuleKt.getActiveSpendableFioAccounts(_mbwManager.getWalletManager(false));
        if (!(account instanceof FioAccount) && !fioAccounts.isEmpty() && fioNames.isEmpty()) {
            menus.add(R.menu.record_options_menu_add_fio_name);
        }

        if (!(account instanceof FioAccount) && !fioNames.isEmpty()) {
            menus.add(R.menu.record_options_menu_my_fio_names);
            menus.add(R.menu.record_options_menu_fio_requests);
        }

        if (account.isActive() && account instanceof HDAccount && !(account instanceof HDPubOnlyAccount)
                && getActiveMasterseedHDAccounts(walletManager).size() > 1 && !isBch) {
            final HDAccount HDAccount = (HDAccount) account;
            BitcoinHDModule bitcoinHDModule = (BitcoinHDModule) walletManager.getModuleById(BitcoinHDModule.ID);
            if (!HDAccount.hasHadActivity() && HDAccount.getAccountIndex() == bitcoinHDModule.getCurrentBip44Index()) {
                //only allow to remove unused HD accounts from the view
                menus.add(R.menu.record_options_menu_hide_unused);
            }
        }

        if (account.isActive() && account instanceof HDAccount) {
            menus.add(R.menu.record_options_boost_gap);
        }

        if (account.getId().equals(_mbwManager.getLocalTraderManager().getLocalTraderAccountId())) {
            menus.add(R.menu.record_options_menu_detach);
        }

        AppCompatActivity parent = (AppCompatActivity) requireActivity();

        Callback actionMode = new Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                for (Integer res : menus) {
                    actionMode.getMenuInflater().inflate(res, menu);
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                MenuItem item = menu.findItem(R.id.miMakeBackup);
                if (item != null) {
                    item.setShowAsAction(AccountViewModel.showBackupMissingWarning(_mbwManager.getSelectedAccount(), _mbwManager) ?
                            MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER);
                }
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.miMapFioAddress:
                        RegisterFioNameActivity.start(requireContext(), account.getId());
                        return true;
                    case R.id.miMapToFio:
                        FioHelper.chooseAccountToMap(requireActivity(), requireFocusedAccount());
                        return true;
                    case R.id.miFIORequests:
                        MbwManager.getEventBus().post(new SelectTab(ModernMain.TAB_FIO_REQUESTS));
                        return true;
                    case R.id.miAboutFIOProtocol:
                        new AboutFIOProtocolDialog().show(getParentFragmentManager(), "modal");
                        break;
                    case R.id.miActivate:
                        activateSelected();
                        return true;
                    case R.id.miSetLabel:
                        setLabelOnAccount(accountListAdapter.getFocusedAccount(), "", true);
                        return true;
                    case R.id.miDeleteRecord:
                        deleteSelected();
                        return true;
                    case R.id.miArchive:
                        archiveSelected();
                        return true;
                    case R.id.miHideUnusedAccount:
                        hideSelected();
                        return true;
                    case R.id.miExport:
                        exportSelectedPrivateKey();
                        return true;
                    case R.id.miSignMessage:
                        signMessage();
                        return true;
                    case R.id.miDetach:
                        detachFromLocalTrader();
                        return true;
                    case R.id.miShowOutputs:
                        showOutputs();
                        return true;
                    case R.id.miMakeBackup:
                        makeBackup();
                        return true;
                    case R.id.miSingleKeyBackupVerify:
                        verifySingleKeyBackup();
                        return true;
                    case R.id.miShamirBackup:
                        try {
                            InMemoryPrivateKey privateKey = account.getPrivateKey(AesKeyCipher.defaultKeyCipher());
                            ShamirSharingActivity.callMe(requireActivity(), privateKey);
                        } catch (KeyCipher.InvalidKeyCipher e) {
                            _toaster.toast("Something went wrong", false);
                        }
                        return true;
                    case R.id.miRescan:
                        // If we are synchronizing, show "Synchronizing, please wait..." to avoid blocking behavior
                        if (requireFocusedAccount().isSyncing()) {
                            _toaster.toast(R.string.synchronizing_please_wait, false);
                            return true;
                        }
                        rescan();
                        return true;
                    case R.id.miBoostGap:
                        boostGapLimitDialog(AccountsFragment.this, _mbwManager, account);
                        return true;
                    default:
                        return false;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                currentActionMode = null;
                // Loose focus
                if (accountListAdapter.getFocusedAccount() != null) {
                    accountListAdapter.setFocusedAccountId(null);
                }
            }
        };
        currentActionMode = parent.startSupportActionMode(actionMode);
        // Late set the focused record. We have to do this after
        // startSupportActionMode above, as it calls onDestroyActionMode when
        // starting for some reason, and this would clear the focus and force
        // an update.
        accountListAdapter.setFocusedAccountId(account.getId());
    }

    private void verifySingleKeyBackup() {
        if (!isAdded()) {
            return;
        }
        WalletAccount account = requireFocusedAccount();
        account.interruptSync();
        if (account instanceof SingleAddressAccount || account instanceof ColuAccount) {
            //start legacy backup verification
            VerifyBackupActivity.callMe(getActivity());
        }
    }

    private void makeBackup() {
        if (!isAdded()) {
            return;
        }
        WalletAccount account = requireFocusedAccount();
        account.interruptSync();
        if (account instanceof ColuAccount) {
            //ColuAccount class can be single or HD
            //TODO: test if account is single address or HD and do wordlist backup instead
            //start legacy backup if a single key or watch only was selected
            Utils.pinProtectedBackup(getActivity());
        } else {
            if (account.isDerivedFromInternalMasterseed()) {
                //start wordlist backup if a HD account or derived account was selected
                Utils.pinProtectedWordlistBackup(getActivity());
            } else if (account instanceof SingleAddressAccount) {
                //start legacy backup if a single key or watch only was selected
                Utils.pinProtectedBackup(getActivity());
            }
        }
    }

    private void showOutputs() {
        WalletAccount account = requireFocusedAccount();
        account.interruptSync();
        Intent intent = new Intent(getActivity(), UnspentOutputsActivity.class)
                .putExtra("account", account.getId());
        startActivity(intent);
    }

    private void signMessage() {
        if (!isAdded()) {
            return;
        }
        runPinProtected(() -> {
            WalletAccount account = accountListAdapter.getFocusedAccount();
            account.interruptSync();
            MessageSigningActivity.callMe(requireContext(), account);
        });
    }

    /**
     * Show a message to the user explaining what it means to select a different
     * address.
     */
    private void toastSelectedAccountChanged(WalletAccount account) {
        if (account.isArchived()) {
            _toaster.toast(getString(R.string.selected_archived_warning), true);
        } else if (account instanceof HDAccount) {
            _toaster.toast(getString(R.string.selected_hd_info), true);
        } else if (account instanceof SingleAddressAccount) {
            _toaster.toast(getString(R.string.selected_single_info), true);
        } else if (account instanceof ColuAccount) {
            _toaster.toast(getString(R.string.selected_colu_info
                    , _mbwManager.getMetadataStorage().getLabelByAccount(account.getId())), true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!isAdded()) {
            return true;
        }
        if (item.getItemId() == R.id.miAddRecord || item.getItemId() == R.id.miAddRecordDuplicate) {
            AddAccountActivity.callMe(this, ADD_RECORD_RESULT_CODE);
            return true;
        } else if (item.getItemId() == R.id.miLockKeys) {
            lock();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setLabelOnAccount(final WalletAccount account, final String defaultName, boolean askForPin) {
        if (account == null || !isAdded()) {
            return;
        }
        if (askForPin) {
            runPinProtected(() -> EnterAddressLabelUtil.enterAccountLabel(requireActivity(), account.getId(), defaultName, _storage));
        } else {
            EnterAddressLabelUtil.enterAccountLabel(requireActivity(), account.getId(), defaultName, _storage);
        }
    }

    private void deleteSelected() {
        if (!isAdded()) {
            return;
        }
        final WalletAccount account = requireFocusedAccount();
        if (account.isActive() && accountProtected(account)) {
            _toaster.toast(R.string.keep_one_active, false);
            return;
        }
        runPinProtected(() -> deleteAccount(account));
    }

    private void rescan() {
        if (!isAdded()) {
            return;
        }
        WalletAccount<?> account = requireFocusedAccount();
        account.dropCachedData();
        _mbwManager.getWalletManager(false)
                .startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED, Collections.singletonList(account));
    }

    private void exportSelectedPrivateKey() {
        if (!isAdded()) {
            return;
        }
        runPinProtected(() -> Utils.exportSelectedAccount(getActivity()));
    }

    private void detachFromLocalTrader() {
        if (!isAdded()) {
            return;
        }
        LocalTraderManager ltm = _mbwManager.getLocalTraderManager();
        boolean hasLt = ltm.hasLocalTraderAccount();
        if (!hasLt) {
            _toaster.toast("No LT configured.", true);
            return;
        }
        runPinProtected(() -> new AlertDialog.Builder(getActivity())
                .setTitle(R.string.lt_detaching_title)
                .setMessage(getString(R.string.lt_detaching_question))
                .setPositiveButton(R.string.yes, (arg0, arg1) -> {
                    WalletAccount wa = walletManager.getAccount(ltm.getLocalTraderAccountId());
                    wa.interruptSync();
                    ltm.unsetLocalTraderAccount();
                    _toaster.toast(R.string.lt_detached, false);
                    update();
                })
                .setNegativeButton(R.string.no, null)
                .show());
    }

    private void activateSelected() {
        if (!isAdded()) {
            return;
        }
        runPinProtected(() -> activate(requireFocusedAccount()));
    }

    private void activate(WalletAccount<?> account) {
        List<WalletAccount<?>> accountsToActivateAndSync = new ArrayList<>();
        accountsToActivateAndSync.add(account);
        if (account instanceof EthAccount) {
            for (WalletAccount<?> walletAccount : getLinkedERC20Accounts(account)) {
                accountsToActivateAndSync.add(walletAccount);
            }
        } else if (account instanceof ERC20Account) {
            EthAccount ethAccount = getLinkedEthAccount(account);
            if (ethAccount.isArchived()) {
                accountsToActivateAndSync.add(ethAccount);
            }
        } else {
            WalletAccount<?> linkedAccount = Utils.getLinkedAccount(account, walletManager.getAccounts());
            if (linkedAccount != null) {
                accountsToActivateAndSync.add(linkedAccount);
            }
        }
        for(WalletAccount<?> wa : accountsToActivateAndSync) {
            wa.activateAccount();
        }
        //setselected also broadcasts AccountChanged event
        _mbwManager.setSelectedAccount(account.getId());
        updateIncludingMenus();
        _toaster.toast(R.string.activated, false);
        _mbwManager.getWalletManager(false)
                .startSynchronization(SyncMode.NORMAL_FORCED, accountsToActivateAndSync);
    }

    private void archiveSelected() {
        if (!isAdded()) {
            return;
        }
        final WalletAccount account = requireFocusedAccount();
        if (accountProtected(account)) {
            //this is the last active hd account, we dont allow archiving it
            _toaster.toast(R.string.keep_one_active, false);
            return;
        }
        if (account instanceof HDAccount) {
            HDAccount hdAccount = (HDAccount) account;
            if (!hdAccount.hasHadActivity() && hdAccount.isDerivedFromInternalMasterseed()) {
                // this hdAccount is unused, we don't allow archiving it
                _toaster.toast(R.string.dont_allow_archiving_unused_notification, false);
                return;
            }
        }
        runPinProtected(() -> archive(account));
    }

    /**
     * Account is protected if after removal no masterseed accounts of the same coin type would stay active,
     * so it would not be possible to select an account
     */
    private boolean accountProtected(WalletAccount toRemove) {
        // accounts not derived from master seed and ethereum account are not protected
        if (!(toRemove.isDerivedFromInternalMasterseed() && toRemove instanceof HDAccount) || toRemove instanceof EthAccount) {
            return false;
        }
        List<WalletAccount<?>> accountsList = getActiveMasterseedAccounts(_mbwManager.getWalletManager(false));
        int cnt = 0;
        for (WalletAccount account : accountsList) {
            if (account.getClass().equals(toRemove.getClass())) {
                cnt++;
            }
        }
        return cnt <= 1;
    }

    private void hideSelected() {
        if (!isAdded()) {
            return;
        }
        final WalletAccount account = requireFocusedAccount();
        if (accountProtected(account)) {
            //this is the last active account, we dont allow hiding it
            _toaster.toast(R.string.keep_one_active, false);
            return;
        }
        if (account instanceof HDAccount) {
            final HDAccount hdAccount = (HDAccount) account;
            if (hdAccount.hasHadActivity() && hdAccount.isDerivedFromInternalMasterseed()) {
                // this hdAccount is used, we don't allow hiding it
                _toaster.toast(R.string.dont_allow_hiding_used_notification, false);
                return;
            }

            runPinProtected(() -> {
                hdAccount.interruptSync();
                _mbwManager.getWalletManager(false).deleteAccount(hdAccount.getId());
                // in case user had labeled the account, delete the stored name
                _storage.deleteAccountMetadata(hdAccount.getId());
                eventBus.post(new AccountChanged(hdAccount.getId()));
                _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveSpendingAccounts().get(0).getId());
                //we dont want to show the context menu for the automatically selected account
                accountListAdapter.setFocusedAccountId(null);
                finishCurrentActionMode();
            });
        }
    }

    private void archive(final WalletAccount account) {
        final List<WalletAccount> linkedAccounts = new ArrayList<>();
        if (account instanceof EthAccount) {
            if (!getActiveLinkedERC20Accounts(account).isEmpty()) {
                linkedAccounts.addAll(getActiveLinkedERC20Accounts(account));
            }
        } else if (!(account instanceof ERC20Account)) {
            if (getLinkedAccount(account) != null) {
                linkedAccounts.add(getLinkedAccount(account));
            }
        }

        Collection<WalletAccount> accountsToInterrupt = new HashSet<>();
        accountsToInterrupt.add(account);
        accountsToInterrupt.addAll(linkedAccounts);
        interruptSync(accountsToInterrupt);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.archiving_account_title)
                .setMessage(Html.fromHtml(createArchiveDialogText(account, linkedAccounts)))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        account.archiveAccount();
                        if (account instanceof EthAccount) {
                            for (WalletAccount walletAccount : getLinkedERC20Accounts(account)) {
                                walletAccount.archiveAccount();
                            }
                        } else if (!(account instanceof ERC20Account)) {
                            WalletAccount linkedAccount = Utils.getLinkedAccount(account, walletManager.getAccounts());
                            if (linkedAccount != null) {
                                linkedAccount.archiveAccount();
                            }
                        }
                        _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveSpendingAccounts().get(0).getId());
                        eventBus.post(new AccountChanged(account.getId()));
                        if (!linkedAccounts.isEmpty()) {
                            for (WalletAccount linkedAccount : linkedAccounts) {
                                eventBus.post(new AccountChanged(linkedAccount.getId()));
                            }
                        }
                        updateIncludingMenus();
                        _toaster.toast(R.string.archived, false);
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @NonNull
    private String createArchiveDialogText(WalletAccount account, List<WalletAccount> linkedAccounts) {
        String accountName = _mbwManager.getMetadataStorage().getLabelByAccount(account.getId());
        return getAccountArchiveText(account, linkedAccounts, accountName);
    }

    @NonNull
    private String getAccountArchiveText(WalletAccount account, List<WalletAccount> linkedAccounts, String accountName) {
        String dialogText;
        Balance balance = checkNotNull(account.getAccountBalance());
        String valueString = getBalanceString(account.getCoinType(), balance);

        // TODO sort linkedAccounts for visible only
        if (linkedAccounts.size() > 1 || ((account instanceof EthAccount) && linkedAccounts.size() > 0)) {
            List<String> linkedAccountStrings = new ArrayList<>();
            for (WalletAccount linkedAccount : linkedAccounts) {
                Balance linkedBalance = linkedAccount.getAccountBalance();
                String linkedAccountName = _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
                String linkedValueString = getBalanceString(linkedAccount.getCoinType(), linkedBalance);
                linkedAccountStrings.add("<b>" + linkedAccountName + "</b> holding <b>" + linkedValueString + "</b>");
            }
            String linkedAccountsString = TextUtils.join(", ", linkedAccountStrings) + "?";
            dialogText = getString(R.string.question_archive_many_accounts, accountName, valueString, linkedAccountsString);
        } else if (!linkedAccounts.isEmpty() && linkedAccounts.get(0).isVisible()) {
            Balance linkedBalance = linkedAccounts.get(0).getAccountBalance();
            String linkedValueString = getBalanceString(linkedAccounts.get(0).getCoinType(), linkedBalance);
            String linkedAccountName = _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccounts.get(0).getId());
            dialogText = getString(R.string.question_archive_account_s, accountName, valueString,
                    linkedAccountName, linkedValueString);
        } else {
            dialogText = getString(R.string.question_archive_account, accountName, valueString);
        }
        return dialogText;
    }

    private void lock() {
        _mbwManager.setKeyManagementLocked(true);
        update();
        if (isAdded()) {
            requireActivity().invalidateOptionsMenu();
        }
    }

    private void runPinProtected(final Runnable runnable) {
        _mbwManager.runPinProtectedFunction(requireActivity(), new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) {
                    return;
                }
                runnable.run();
            }
        });
    }

    OnClickListener unlockClickedListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {
                @Override
                public void run() {
                    _mbwManager.setKeyManagementLocked(false);
                    update();
                    if (isAdded()) {
                        requireActivity().invalidateOptionsMenu();
                    }
                }
            });
        }
    };

    @NonNull
    private WalletAccount requireFocusedAccount() {
        return checkNotNull(accountListAdapter.getFocusedAccount());
    }

    @Subscribe
    public void addressChanged(ReceivingAddressChanged event) {
        update();
    }

    @Subscribe
    public void balanceChanged(BalanceChanged event) {
        update();
    }

    @Subscribe
    public void syncFailed(SyncFailed event) {
        update();
    }

    @Subscribe
    public void syncStarted(SyncStarted event) {
        update();
    }

    @Subscribe
    public void syncStopped(SyncStopped event) {
        update();
    }

    @Subscribe
    public void accountChanged(AccountChanged event) {
        update();
    }

    @Subscribe
    public void syncProgressUpdated(SyncProgressUpdated event) {
        update();
    }

    @Subscribe
    public void exchangeSourceChange(ExchangeSourceChanged event) {
        accountListAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
        accountListAdapter.notifyDataSetChanged();
    }

    @Subscribe
    public void selectedCurrencyChanged(SelectedCurrencyChanged event) {
        accountListAdapter.notifyDataSetChanged();
    }

    private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {
        @Override
        public void onLtError(int errorCode) {
        }

        @Override
        public void onLtAccountDeleted(DeleteTrader request) {
            accountListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLtTraderCreated(CreateTrader request) {
            accountListAdapter.notifyDataSetChanged();
        }
    };
}
