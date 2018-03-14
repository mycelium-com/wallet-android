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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.AddAccountActivity;
import com.mycelium.wallet.activity.AddAdvancedAccountActivity;
import com.mycelium.wallet.activity.AddCoinapultAccountActivity;
import com.mycelium.wallet.activity.MessageSigningActivity;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.modern.adapter.AccountListAdapter;
import com.mycelium.wallet.activity.view.DividerItemDecoration;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.coinapult.CoinapultAccount;
import com.mycelium.wallet.coinapult.CoinapultManager;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.ColuManager;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.ExtraAccountsChanged;
import com.mycelium.wallet.event.ReceivingAddressChanged;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44PubOnlyAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.UUID;

public class AccountsFragment extends Fragment {
   public static final int ADD_RECORD_RESULT_CODE = 0;

   public static final String TAG = "AccountsFragment";

   private WalletManager walletManager;

   private MetadataStorage _storage;
   private MbwManager _mbwManager;
   private Toaster _toaster;
   private ProgressDialog _progress;
   private RecyclerView rvRecords;
   private View llLocked;
   private AccountListAdapter accountListAdapter;

   /**
    * Called when the activity is first created.
    */
   @SuppressWarnings("deprecation")
   @Override
   public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      return inflater.inflate(R.layout.records_activity, container, false);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
   }

   @Override
   public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      rvRecords = view.findViewById(R.id.rvRecords);
      rvRecords.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
      accountListAdapter = new AccountListAdapter(getActivity(), _mbwManager);
      rvRecords.setAdapter(accountListAdapter);
      rvRecords.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.divider_account_list)
              , LinearLayoutManager.VERTICAL));
      rvRecords.setHasFixedSize(true);
      llLocked = view.findViewById(R.id.llLocked);
      accountListAdapter.setItemClickListener(recordAddressClickListener);
      accountListAdapter.setItemSelectListener(recordStarClickListener);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(activity);
      walletManager = _mbwManager.getWalletManager(false);
      _storage = _mbwManager.getMetadataStorage();
      _toaster = new Toaster(this);
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      getView().findViewById(R.id.btUnlock).setOnClickListener(unlockClickedListener);
      update();
      _progress = new ProgressDialog(getActivity());
      super.onResume();
   }

   @Override
   public void onPause() {
      _progress.dismiss();
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
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
      ActivityCompat.invalidateOptionsMenu(getActivity());
      if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == AddCoinapultAccountActivity.RESULT_COINAPULT) {
         UUID accountId = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
         CoinapultAccount account = (CoinapultAccount) _mbwManager.getWalletManager(false).getAccount(accountId);
         _mbwManager.setSelectedAccount(accountId);
         accountListAdapter.setFocusedAccount(account);
         updateIncludingMenus();

      } else if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         UUID accountid = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
         //check whether the account is active - we might have scanned the priv key for an archived watchonly
         WalletManager walletManager = _mbwManager.getWalletManager(false);
         WalletAccount account = walletManager.getAccount(accountid);
         if (account.isActive()) {
            _mbwManager.setSelectedAccount(accountid);
         }
         accountListAdapter.setFocusedAccount(account);
         updateIncludingMenus();
         if(account.getType() != WalletAccount.Type.COLU && !intent.getBooleanExtra(AddAccountActivity.IS_UPGRADE, false)) {
            setNameForNewAccount(account);
         }
         _mbwManager.getEventBus().post(new ExtraAccountsChanged());
         _mbwManager.getEventBus().post(new AccountChanged(accountid));
      } else if(requestCode == ADD_RECORD_RESULT_CODE && resultCode == AddAdvancedAccountActivity.RESULT_MSG) {
         new AlertDialog.Builder(getActivity())
                 .setMessage(intent.getStringExtra(AddAccountActivity.RESULT_MSG))
                 .setPositiveButton(R.string.button_ok, null)
                 .create()
                 .show();
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void deleteAccount(final WalletAccount accountToDelete) {
      Preconditions.checkNotNull(accountToDelete);

      final View checkBoxView = View.inflate(getActivity(), R.layout.delkey_checkbox, null);
      final CheckBox keepAddrCheckbox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
      keepAddrCheckbox.setText(getString(R.string.keep_account_address));
      keepAddrCheckbox.setChecked(false);

      final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getActivity());
      deleteDialog.setTitle(R.string.delete_account_title);
      final WalletAccount account = _mbwManager.getSelectedAccount();
      final WalletAccount linkedAccount = Utils.getLinkedAccount(account, _mbwManager.getColuManager().getAccounts().values());
      if (account instanceof ColuAccount) {
         deleteDialog.setMessage(getString(R.string.delete_account_message)
                 + "\n" + getString(R.string.both_rmc_will_deleted
                 , _mbwManager.getMetadataStorage().getLabelByAccount(account.getId())
                 , _mbwManager.getMetadataStorage().getLabelByAccount(((ColuAccount) account).getLinkedAccount().getId())));
      } else if (linkedAccount != null) {
         deleteDialog.setMessage(getString(R.string.delete_account_message)
                 + "\n" + getString(R.string.both_rmc_will_deleted
                 , _mbwManager.getMetadataStorage().getLabelByAccount(account.getId())
                 , _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId())));
      } else {
         deleteDialog.setMessage(R.string.delete_account_message);
      }

      // add checkbox only for SingleAddressAccounts and only if a private key is present
      final boolean hasPrivateData = (accountToDelete instanceof ExportableAccount
            && ((ExportableAccount) accountToDelete).getExportData(AesKeyCipher.defaultKeyCipher()).privateData.isPresent());

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
            if (hasPrivateData) {
               Long satoshis = getPotentialBalance(accountToDelete);
               AlertDialog.Builder confirmDeleteDialog = new AlertDialog.Builder(getActivity());
               confirmDeleteDialog.setTitle(R.string.confirm_delete_pk_title);

               // Set the message. There are four combinations, with and without label, with and without BTC amount.
               String label = _mbwManager.getMetadataStorage().getLabelByAccount(accountToDelete.getId());
               int labelCount = 1;
               if (account instanceof ColuAccount) {
                  label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(((ColuAccount) account).getLinkedAccount().getId());
                  labelCount++;
               } else if (linkedAccount != null) {
                  label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
                  labelCount++;
               }
               String message;

               // For active accounts we check whether there is money on them before deleting. we don't know if there
               // is money on archived accounts
               Optional<Address> receivingAddress = accountToDelete.getReceivingAddress();
               if (accountToDelete.isActive() && satoshis != null && satoshis > 0) {
                  if (label != null && label.length() != 0) {
                     String address;
                     if (receivingAddress.isPresent()) {
                        address = receivingAddress.get().toMultiLineString();
                     } else {
                        address = "";
                     }
                     message = getString(R.string.confirm_delete_pk_with_balance_with_label
                             , getResources().getQuantityString(R.plurals.account_label, labelCount, label)
                             , address, accountToDelete instanceof ColuAccount ?
                                     Utils.getColuFormattedValueWithUnit(getPotentialBalanceColu(accountToDelete))
                                     : _mbwManager.getBtcValueString(satoshis)
                     );
                  } else {
                     message = getString(
                           R.string.confirm_delete_pk_with_balance,
                           receivingAddress.isPresent() ? receivingAddress.get().toMultiLineString() : "",
                             accountToDelete instanceof ColuAccount ?
                                     Utils.getColuFormattedValueWithUnit(getPotentialBalanceColu(accountToDelete))
                                     : _mbwManager.getBtcValueString(satoshis)

                     );
                  }
               } else {
                  if (label != null && label.length() != 0) {
                     message = getString(R.string.confirm_delete_pk_without_balance_with_label
                             ,getResources().getQuantityString(R.plurals.account_label, labelCount, label)
                           ,receivingAddress.isPresent() ? receivingAddress.get().toMultiLineString() : ""
                     );
                  } else {
                     message = getString(
                           R.string.confirm_delete_pk_without_balance,
                           receivingAddress.isPresent() ? receivingAddress.get().toMultiLineString() : ""
                     );
                  }
               }
               confirmDeleteDialog.setMessage(message);

               confirmDeleteDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                     Log.d(TAG, "In deleteFragment onClick");
                     if (keepAddrCheckbox.isChecked() && accountToDelete instanceof SingleAddressAccount) {
                        try {
                           //Check if this SingleAddress account is related with ColuAccount
                           WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, _mbwManager.getColuManager().getAccounts().values());
                           if (linkedColuAccount != null && linkedColuAccount instanceof ColuAccount) {
                              ColuManager coluManager = _mbwManager.getColuManager();
                              coluManager.forgetPrivateKey((ColuAccount) linkedColuAccount);
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
                              ColuManager coluManager = _mbwManager.getColuManager();
                              if (keepAddrCheckbox.isChecked()) {
                                 coluManager.forgetPrivateKey((ColuAccount) accountToDelete);
                              } else {
                                 coluManager.deleteAccount((ColuAccount) accountToDelete);
                                 _toaster.toast("Deleting account.", false);
                                 _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                                 _mbwManager.getEventBus().post(new ExtraAccountsChanged()); // do we need to pass UUID ?
                              }
                           } catch (Exception e) {
                              // make a message !
                              _toaster.toast(getString(R.string.colu_error_deleting), false);
                           }
                        } else {
                           //Check if this SingleAddress account is related with ColuAccount
                           WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, _mbwManager.getColuManager().getAccounts().values());
                           if (linkedColuAccount != null && linkedColuAccount instanceof ColuAccount) {
                              ColuManager coluManager = _mbwManager.getColuManager();
                              coluManager.deleteAccount((ColuAccount) linkedColuAccount);
                           } else {
                              try {
                                 walletManager.deleteUnrelatedAccount(accountToDelete.getId(), AesKeyCipher.defaultKeyCipher());
                                 _storage.deleteAccountMetadata(accountToDelete.getId());
                              } catch (KeyCipher.InvalidKeyCipher e) {
                                 throw new RuntimeException(e);
                              }
                           }
                           _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                           _toaster.toast(R.string.account_deleted, false);
                           _mbwManager.getEventBus().post(new ExtraAccountsChanged());
                        }
                     }
                     finishCurrentActionMode();
                     _mbwManager.getEventBus().post(new AccountChanged(accountToDelete.getId()));
                  }
               });
               confirmDeleteDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                  }
               });
               confirmDeleteDialog.show();
            } else {
               // account has no private data - dont make a fuzz about it and just delete it
               if (accountToDelete instanceof ColuAccount) {
                  ColuManager coluManager = _mbwManager.getColuManager();
                  coluManager.deleteAccount((ColuAccount) accountToDelete);
               } else {
                  //Check if this SingleAddress account is related with ColuAccount
                  WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, _mbwManager.getColuManager().getAccounts().values());
                  if (linkedColuAccount != null && linkedColuAccount instanceof ColuAccount) {
                     ColuManager coluManager = _mbwManager.getColuManager();
                     coluManager.deleteAccount((ColuAccount) linkedColuAccount);
                  } else {
                     try {
                        walletManager.deleteUnrelatedAccount(accountToDelete.getId(), AesKeyCipher.defaultKeyCipher());
                        _storage.deleteAccountMetadata(accountToDelete.getId());
                     } catch (KeyCipher.InvalidKeyCipher e) {
                        throw new RuntimeException(e);
                     }
                  }
               }
               finishCurrentActionMode();
               _mbwManager.getEventBus().post(new AccountChanged(accountToDelete.getId()));
               _mbwManager.getEventBus().post(new ExtraAccountsChanged());
               _toaster.toast(R.string.account_deleted, false);
            }
         }

         private CurrencyValue getPotentialBalanceColu(WalletAccount account) {
            if (account.isArchived()) {
               return null;
            } else {
               CurrencyBasedBalance balance = account.getCurrencyBasedBalance();
               return balance.confirmed;
            }
         }

         private Long getPotentialBalance(WalletAccount account) {
            if (account.isArchived()) {
               return null;
            } else {
               Balance balance = account.getBalance();
               return balance.confirmed + balance.pendingChange + balance.pendingReceiving;
            }
         }

      });
      deleteDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
         }
      });
      deleteDialog.show();

   }

   private void finishCurrentActionMode() {
      if (currentActionMode != null) {
         currentActionMode.finish();
      }
   }

   private void setNameForNewAccount(WalletAccount account) {
      if (account == null || !isAdded()) {
         return;
      }
      String baseName = Utils.getNameForNewAccount(account, getActivity());
      //append counter if name already in use
      String defaultName = baseName;
      int num = 1;
      while (_storage.getAccountByLabel(defaultName).isPresent()) {
         defaultName = baseName + " (" + num++ + ')';
      }
      //we just put the default name into storage first, if there is none
      //if the user cancels entry or it gets somehow aborted, we at least have a valid entry
      if (_mbwManager.getMetadataStorage().getLabelByAccount(account.getId()).length() == 0) {
         _mbwManager.getMetadataStorage().storeAccountLabel(account.getId(), defaultName);
      }
      setLabelOnAccount(account, defaultName, false);
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
         //this sould fix crash when delete account
         rvRecords.post(new Runnable() {
            @Override
            public void run() {
               accountListAdapter.updateData();
            }
         });
      }
   }

   private ActionMode currentActionMode;

   private AccountListAdapter.ItemSelectListener recordStarClickListener = new AccountListAdapter.ItemSelectListener() {

      @Override
      public void onClick(WalletAccount account) {
         if (account.isActive()) {
            _mbwManager.setSelectedAccount(account.getId());
         }
         update();
      }
   };

   private AccountListAdapter.ItemClickListener recordAddressClickListener = new AccountListAdapter.ItemClickListener() {
      @Override
      public void onItemClick(WalletAccount account) {
         // Check whether a new account was selected
         if (!_mbwManager.getSelectedAccount().equals(account) && account.isActive()) {
            _mbwManager.setSelectedAccount(account.getId());
         }
         toastSelectedAccountChanged(account);
         updateIncludingMenus();
      }
   };

   private void updateIncludingMenus() {
      WalletAccount account = accountListAdapter.getFocusedAccount();
      boolean isBch = account.getType() == WalletAccount.Type.BCHSINGLEADDRESS
              || account.getType() == WalletAccount.Type.BCHBIP44;

      final List<Integer> menus = Lists.newArrayList();
      if(!(account instanceof ColuAccount)
              && !Utils.checkIsLinked(account, _mbwManager.getColuManager().getAccounts().values()) ) {
         menus.add(R.menu.record_options_menu);
      }

      if ((account instanceof SingleAddressAccount) || (account.isDerivedFromInternalMasterseed())) {
         menus.add(R.menu.record_options_menu_backup);
      }

      if (account instanceof SingleAddressAccount) {
         menus.add(R.menu.record_options_menu_backup_verify);
      }

      if(account instanceof ColuAccount) {
         //TODO: distinguish between ColuAccount in single address mode and HD mode
         menus.add(R.menu.record_options_menu_backup);
         menus.add(R.menu.record_options_menu_backup_verify);
      }

      if (!account.isDerivedFromInternalMasterseed() && !isBch) {
         menus.add(R.menu.record_options_menu_delete);
      }

      if (account.isActive() && account.canSpend() && !(account instanceof Bip44PubOnlyAccount)
              && !isBch) {
         menus.add(R.menu.record_options_menu_sign);
      }

      if (account.isActive() && !isBch) {
         menus.add(R.menu.record_options_menu_active);
      }

      if (account.isActive() && !(account instanceof CoinapultAccount) && !isBch) {
         menus.add(R.menu.record_options_menu_outputs);
      }

      if (account instanceof CoinapultAccount) {
         menus.add(R.menu.record_options_menu_set_coinapult_mail);
      }

      if (account.isArchived()) {
         menus.add(R.menu.record_options_menu_archive);
      }

      if (account.isActive() && account instanceof ExportableAccount && !isBch) {
         menus.add(R.menu.record_options_menu_export);
      }

      if (account.isActive() && account instanceof Bip44Account && !(account instanceof Bip44PubOnlyAccount)
              && AccountManager.INSTANCE.getBTCMasterSeedAccounts().size() > 1 && !isBch) {

         if (!((Bip44Account) account).hasHadActivity()) {
            //only allow to remove unused HD acounts from the view
            menus.add(R.menu.record_options_menu_hide_unused);
         }
      }

      if (RecordRowBuilder.showLegacyAccountWarning(account, _mbwManager)) {
         menus.add(R.menu.record_options_menu_ignore_warning);
      }

      if (account.getId().equals(_mbwManager.getLocalTraderManager().getLocalTraderAccountId())) {
         menus.add(R.menu.record_options_menu_detach);
      }

      AppCompatActivity parent = (AppCompatActivity) getActivity();

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
            return true;
         }

         @Override
         public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            // If we are synchronizing, show "Synchronizing, please wait..." to avoid blocking behavior
            if (_mbwManager.getWalletManager(false).getState() == WalletManager.State.SYNCHRONIZING
                    || _mbwManager.getColuManager().getState() == WalletManager.State.SYNCHRONIZING) {
               _toaster.toast(R.string.synchronizing_please_wait, false);
               return true;
            }
            int id = menuItem.getItemId();
            if (id == R.id.miActivate) {
               activateSelected();
               return true;
            } else if (id == R.id.miSetLabel) {
               setLabelOnAccount(accountListAdapter.getFocusedAccount(), "", true);
               return true;
            } else if (id == R.id.miDeleteRecord) {
               deleteSelected();
               return true;
            } else if (id == R.id.miArchive) {
               archiveSelected();
               return true;
            } else if (id == R.id.miHideUnusedAccount) {
               hideSelected();
               return true;
            } else if (id == R.id.miExport) {
               exportSelectedPrivateKey();
               return true;
            } else if (id == R.id.miIgnoreWarnings) {
               ignoreSelectedPrivateKey();
               return true;
            } else if (id == R.id.miSignMessage) {
               signMessage();
               return true;
            } else if (id == R.id.miDetach) {
               detachFromLocalTrader();
               return true;
            } else if (id == R.id.miShowOutputs) {
               showOutputs();
               return true;
            } else if (id == R.id.miMakeBackup) {
               makeBackup();
               return true;
            } else if (id == R.id.miSingleKeyBackupVerify) {
               verifySingleKeyBackup();
               return true;
            } else if (id == R.id.miRescan) {
               rescan();
               return true;
            } else if (id == R.id.miSetMail) {
               setCoinapultMail();
               return true;
            } else if (id == R.id.miVerifyMail) {
               verifyCoinapultMail();
               return true;
            }
            return false;
         }

         @Override
         public void onDestroyActionMode(ActionMode actionMode) {
            currentActionMode = null;
            // Loose focus
            if (accountListAdapter.getFocusedAccount() != null) {
               accountListAdapter.setFocusedAccount(null);
               update();
            }
         }
      };
      currentActionMode = parent.startSupportActionMode(actionMode);
      // Late set the focused record. We have to do this after
      // startSupportActionMode above, as it calls onDestroyActionMode when
      // starting for some reason, and this would clear the focus and force
      // an update.
      accountListAdapter.setFocusedAccount(account);

      update();
   }

   //todo: maybe move it to another class along with the other coinaspult mail stuff? would require passing the context for dialog boxes though.
   private void setCoinapultMail() {

      AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
      b.setTitle(getString(R.string.coinapult_mail_question));
      View diaView = getActivity().getLayoutInflater().inflate(R.layout.ext_coinapult_mail, null);
      final EditText mailField = (EditText) diaView.findViewById(R.id.mail);
      mailField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
      String email = _mbwManager.getMetadataStorage().getCoinapultMail();
      if (!email.isEmpty()) {
         mailField.setText(email);
      }
      b.setView(diaView);
      b.setPositiveButton(getString(R.string.button_done), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            String mailText = mailField.getText().toString();
            if (Utils.isValidEmailAddress(mailText)) {
               Optional<String> mail;
               if (mailText.isEmpty()) {
                  mail = Optional.absent();
               } else {
                  mail = Optional.of(mailText);
               }
               _progress.setCancelable(false);
               _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
               _progress.setMessage(getString(R.string.coinapult_setting_email));
               _progress.show();
               _mbwManager.getMetadataStorage().setCoinapultMail(mailText);
               new SetCoinapultMailAsyncTask(mail).execute();
               dialog.dismiss();
            } else {
               new Toaster(AccountsFragment.this).toast("Email address not valid", false);
            }
         }
      });
      b.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
         }
      });

      AlertDialog dialog = b.create();
      dialog.show();
   }

   private void verifyCoinapultMail() {
      AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
      b.setTitle(getString(R.string.coinapult_mail_verification));
      final String email = _mbwManager.getMetadataStorage().getCoinapultMail();
      View diaView = getActivity().getLayoutInflater().inflate(R.layout.ext_coinapult_mail_verification, null);
      final EditText verificationTextField = (EditText) diaView.findViewById(R.id.mailVerification);

      // check if there is a probable verification link in the clipboard and if so, pre-fill the textbox
      String clipboardString = Utils.getClipboardString(getActivity());
      if (!Strings.isNullOrEmpty(clipboardString) && clipboardString.contains("coinapult.com")) {
         verificationTextField.setText(clipboardString);
      }

      b.setView(diaView);
      b.setPositiveButton(getString(R.string.button_done), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            String verification = verificationTextField.getText().toString();
            _progress.setCancelable(false);
            _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            _progress.setMessage(getString(R.string.coinapult_verifying_email));
            _progress.show();
            new VerifyCoinapultMailAsyncTask(verification, email).execute();
            dialog.dismiss();
         }
      });
      b.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
         }
      });

      AlertDialog dialog = b.create();
      dialog.show();
   }

   private class SetCoinapultMailAsyncTask extends AsyncTask<Void, Integer, Boolean> {
      private Optional<String> mail;

      public SetCoinapultMailAsyncTask(Optional<String> mail) {
         this.mail = mail;
      }

      @Override
      protected Boolean doInBackground(Void... params) {
         return _mbwManager.getCoinapultManager().setMail(mail);
      }

      @Override
      protected void onPostExecute(Boolean success) {
         _progress.dismiss();
         if (success) {
            Utils.showSimpleMessageDialog(getActivity(), R.string.coinapult_set_mail_please_verify);
         } else {
            Utils.showSimpleMessageDialog(getActivity(), R.string.coinapult_set_mail_failed);
         }
      }
   }

   private class VerifyCoinapultMailAsyncTask extends AsyncTask<Void, Integer, Boolean> {
      private String link;
      private String email;

      public VerifyCoinapultMailAsyncTask(String link, String email) {
         this.link = link;
         this.email = email;
      }

      @Override
      protected Boolean doInBackground(Void... params) {
         return _mbwManager.getCoinapultManager().verifyMail(link, email);
      }

      @Override
      protected void onPostExecute(Boolean success) {
         _progress.dismiss();
         if (success) {
            Utils.showSimpleMessageDialog(getActivity(), R.string.coinapult_verify_mail_success);
         } else {
            Utils.showSimpleMessageDialog(getActivity(), R.string.coinapult_verify_mail_error);
         }
      }
   }


   private void verifySingleKeyBackup() {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if (_focusedAccount instanceof SingleAddressAccount || _focusedAccount instanceof ColuAccount) {
         //start legacy backup verification
         VerifyBackupActivity.callMe(getActivity());
      }
   }

   private void makeBackup() {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if(_focusedAccount instanceof ColuAccount) {
         //ColuAccount class can be single or HD
         //TODO: test if account is single address or HD and do wordlist backup instead
         //start legacy backup if a single key or watch only was selected
         Utils.pinProtectedBackup(getActivity());
      } else {
         if (_focusedAccount.isDerivedFromInternalMasterseed()) {
            //start wordlist backup if a HD account or derived account was selected
            Utils.pinProtectedWordlistBackup(getActivity());
         } else if (_focusedAccount instanceof SingleAddressAccount) {
            //start legacy backup if a single key or watch only was selected
            Utils.pinProtectedBackup(getActivity());
         }
      }
   }

   private void showOutputs() {
      Intent intent = new Intent(getActivity(), UnspentOutputsActivity.class);
      WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      intent.putExtra("account", _focusedAccount.getId());
      startActivity(intent);
   }

   private void signMessage() {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
            if (_focusedAccount instanceof CoinapultAccount) {
               CoinapultManager coinapultManager = _mbwManager.getCoinapultManager();
               MessageSigningActivity.callMe(getActivity(), coinapultManager.getAccountKey());
            } else if (_focusedAccount instanceof SingleAddressAccount) {
               MessageSigningActivity.callMe(getActivity(), (SingleAddressAccount) _focusedAccount);
            } else if(_focusedAccount instanceof ColuAccount){
               MessageSigningActivity.callMe(getActivity(), ((ColuAccount) _focusedAccount).getPrivateKey());
            } else {
               Intent intent = new Intent(getActivity(), HDSigningActivity.class);
               intent.putExtra("account", _focusedAccount.getId());
               startActivity(intent);
            }
         }
      });
   }

   /**
    * Show a message to the user explaining what it means to select a different
    * address.
    */
   private void toastSelectedAccountChanged(WalletAccount account) {
      if (account.isArchived()) {
         _toaster.toast(getString(R.string.selected_archived_warning), true);
      } else if (account instanceof Bip44Account) {
         _toaster.toast(getString(R.string.selected_hd_info), true);
      } else if (account instanceof SingleAddressAccount) {
         _toaster.toast(getString(R.string.selected_single_info), true);
      } else if(account instanceof ColuAccount) {
          _toaster.toast(getString(R.string.selected_colu_info
                  , _mbwManager.getMetadataStorage().getLabelByAccount(account.getId())), true);
      }
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      // If we are synchronizing, show "Synchronizing, please wait..." to avoid blocking behavior
      if (_mbwManager.getWalletManager(false).getState() == WalletManager.State.SYNCHRONIZING
              || _mbwManager.getColuManager().getState() == WalletManager.State.SYNCHRONIZING) {
         _toaster.toast(R.string.synchronizing_please_wait, false);
         return true;
      }

      if (!isAdded()) {
         return true;
      }
      if (item.getItemId() == R.id.miAddRecord) {
         AddAccountActivity.callMe(this, ADD_RECORD_RESULT_CODE);
         return true;
      } else if (item.getItemId() == R.id.miAddFiatAccount) {
         Intent intent = AddCoinapultAccountActivity.getIntent(getActivity());
         this.startActivityForResult(intent, ADD_RECORD_RESULT_CODE);
         return true;
      } else if (item.getItemId() == R.id.miLockKeys) {
         lock();
         return true;
      }

      return super.onOptionsItemSelected(item);
   }

   private void setLabelOnAccount(final WalletAccount account, final String defaultName, boolean askForPin) {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      if (askForPin) {
         _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

            @Override
            public void run() {
               if (!AccountsFragment.this.isAdded()) {
                  return;
               }
               EnterAddressLabelUtil.enterAccountLabel(getActivity(), account.getId(), defaultName, _storage);
            }

         });
      } else {
         EnterAddressLabelUtil.enterAccountLabel(getActivity(), account.getId(), defaultName, _storage);
      }
   }

   private void deleteSelected() {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      final WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if (_focusedAccount.isActive() && _mbwManager.getWalletManager(false).getActiveAccounts().size() < 2) {
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            deleteAccount(_focusedAccount);
         }

      });
   }

   private void rescan() {
      if (!isAdded()) {
         return;
      }
      accountListAdapter.getFocusedAccount().dropCachedData();
      _mbwManager.getWalletManager(false).startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED);
      _mbwManager.getColuManager().startSynchronization();
   }

   private void ignoreSelectedPrivateKey() {
      if (!isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
            confirmDialog.setTitle(R.string.ignore_warnings_title);
            confirmDialog.setMessage(getString(R.string.ignore_warnings_description));
            final WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
            confirmDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

               public void onClick(DialogInterface arg0, int arg1) {
                  _mbwManager.getMetadataStorage().setIgnoreLegacyWarning(_focusedAccount.getId(), true);
                  _mbwManager.getEventBus().post(new AccountChanged(_focusedAccount.getId()));
               }
            });
            confirmDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface arg0, int arg1) {
                  _mbwManager.getMetadataStorage().setIgnoreLegacyWarning(_focusedAccount.getId(), false);
                  _mbwManager.getEventBus().post(new AccountChanged(_focusedAccount.getId()));
               }
            });
            confirmDialog.show();
         }

      });
   }

   private void exportSelectedPrivateKey() {
      if (!isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            Utils.exportSelectedAccount(AccountsFragment.this.getActivity());
         }

      });
   }

   private void detachFromLocalTrader() {
      if (!isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
            confirmDialog.setTitle(R.string.lt_detaching_title);
            confirmDialog.setMessage(getString(R.string.lt_detaching_question));
            confirmDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

               public void onClick(DialogInterface arg0, int arg1) {
                  _mbwManager.getLocalTraderManager().unsetLocalTraderAccount();
                  _toaster.toast(R.string.lt_detached, false);
                  update();
               }
            });
            confirmDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

               public void onClick(DialogInterface arg0, int arg1) {
               }
            });
            confirmDialog.show();
         }

      });
   }

   private void activateSelected() {
      if (!isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            activate(accountListAdapter.getFocusedAccount());
         }

      });
   }

   private void activate(WalletAccount account) {
      account.activateAccount();
      WalletAccount linkedAccount = Utils.getLinkedAccount(account, _mbwManager.getColuManager().getAccounts().values());
      if (linkedAccount != null) {
         linkedAccount.activateAccount();
      }
      //setselected also broadcasts AccountChanged event
      _mbwManager.setSelectedAccount(account.getId());
      updateIncludingMenus();
      _toaster.toast(R.string.activated, false);
      _mbwManager.getWalletManager(false).startSynchronization();
   }

   private void archiveSelected() {
      if (!isAdded()) {
         return;
      }
      if (_mbwManager.getWalletManager(false).getActiveAccounts().size() < 2) {
         //this is the last active account, we dont allow archiving it
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
      final WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if (_focusedAccount instanceof CoinapultAccount) {
         _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

            @Override
            public void run() {
               if (!AccountsFragment.this.isAdded()) {
                  return;
               }

               archive(_focusedAccount);
            }

         });
         return;
      }
      if (_focusedAccount instanceof Bip44Account) {
         Bip44Account account = (Bip44Account) _focusedAccount;
         if (!account.hasHadActivity()) {
            //this account is unused, we dont allow archiving it
            _toaster.toast(R.string.dont_allow_archiving_unused_notification, false);
            return;
         }
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }

            archive(_focusedAccount);
         }

      });
   }

   private void hideSelected() {
      if (!isAdded()) {
         return;
      }
      if (_mbwManager.getWalletManager(false).getActiveAccounts().size() < 2) {
         //this is the last active account, we dont allow hiding it
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
      final WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if (_focusedAccount instanceof Bip44Account) {
         final Bip44Account account = (Bip44Account) _focusedAccount;
         if (account.hasHadActivity()) {
            //this account is used, we don't allow hiding it
            _toaster.toast(R.string.dont_allow_hiding_used_notification, false);
            return;
         }

         _mbwManager.runPinProtectedFunction(this.getActivity(), new Runnable() {
            @Override
            public void run() {
               _mbwManager.getWalletManager(false).removeUnusedBip44Account(account);
               //in case user had labeled the account, delete the stored name
               _storage.deleteAccountMetadata(account.getId());
               //setselected also broadcasts AccountChanged event, which will cause an ui update
               _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
               //we dont want to show the context menu for the automatically selected account
               accountListAdapter.setFocusedAccount(null);
               finishCurrentActionMode();
            }
         });

      }
   }

   private void archive(final WalletAccount account) {
      new AlertDialog.Builder(getActivity())
              .setTitle(R.string.archiving_account_title)
              .setMessage(getString(R.string.question_archive_account))
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                 public void onClick(DialogInterface arg0, int arg1) {
                    account.archiveAccount();
                    WalletAccount linkedAccount = Utils.getLinkedAccount(account, _mbwManager.getColuManager().getAccounts().values());
                    if (linkedAccount != null) {
                       linkedAccount.archiveAccount();
                    }
                    _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                    _mbwManager.getEventBus().post(new AccountChanged(account.getId()));
                    updateIncludingMenus();
                    _toaster.toast(R.string.archived, false);
                 }
              })
              .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface arg0, int arg1) {
                 }
              })
              .show();
   }

   private void lock() {
      _mbwManager.setKeyManagementLocked(true);
      update();
      if (isAdded()) {
         getActivity().supportInvalidateOptionsMenu();
      }
   }

   OnClickListener unlockClickedListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

            @Override
            public void run() {
               _mbwManager.setKeyManagementLocked(false);
               update();
               if (isAdded()) {
                  getActivity().supportInvalidateOptionsMenu();
               }
            }

         });
      }
   };

   @Subscribe()
   public void onExtraAccountsChanged(ExtraAccountsChanged event) {
      update();
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
   public void syncStarted(SyncStarted event) {
   }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      update();
   }

   @Subscribe
   public void accountChanged(AccountChanged event) {
      update();
   }

}
