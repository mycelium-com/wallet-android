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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArraySet;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
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
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.AddAccountActivity;
import com.mycelium.wallet.activity.AddAdvancedAccountActivity;
import com.mycelium.wallet.activity.MessageSigningActivity;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.modern.adapter.AccountListAdapter;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.view.DividerItemDecoration;
import com.mycelium.wallet.coinapult.CoinapultAccount;
import com.mycelium.wallet.coinapult.CoinapultManager;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.ColuManager;
import com.mycelium.wallet.event.*;
import com.mycelium.wallet.lt.LocalTraderEventSubscriber;
import com.mycelium.wallet.lt.LocalTraderManager;
import com.mycelium.wallet.lt.api.CreateTrader;
import com.mycelium.wallet.lt.api.DeleteTrader;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.mycelium.wapi.wallet.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.bip44.HDAccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
         root = inflater.inflate(R.layout.records_activity, container, false);
      }
      return root;
   }

   @Override
   public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      if (rvRecords == null) {
         rvRecords = view.findViewById(R.id.rvRecords);
         rvRecords.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
         accountListAdapter = new AccountListAdapter(this, _mbwManager);
         rvRecords.setAdapter(accountListAdapter);
         rvRecords.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.divider_account_list)
                 , LinearLayoutManager.VERTICAL));
         rvRecords.setHasFixedSize(true);
      }
      if (llLocked == null) {
         llLocked = view.findViewById(R.id.llLocked);
      }
      accountListAdapter.setItemClickListener(recordAddressClickListener);
   }

   @Override
   public void onAttach(Context context) {
      _mbwManager = MbwManager.getInstance(context);
      walletManager = _mbwManager.getWalletManager(false);
      localTraderManager = _mbwManager.getLocalTraderManager();
      localTraderManager.subscribe(ltSubscriber);
      _storage = _mbwManager.getMetadataStorage();
      eventBus = _mbwManager.getEventBus();
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
      ActivityCompat.invalidateOptionsMenu(getActivity());
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
            if (account.getType() != WalletAccount.Type.COLU && !intent.getBooleanExtra(AddAccountActivity.IS_UPGRADE, false)) {
               setNameForNewAccount(account);
            }
            if (account.getType() == WalletAccount.Type.BTCSINGLEADDRESS
                    && intent.getBooleanExtra(AddAccountActivity.IS_UPGRADE, false)) {
                setNameForUpgradeAccount(account);
            }
            eventBus.post(new ExtraAccountsChanged());
            eventBus.post(new AccountChanged(accountid));
         }
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
       final WalletAccount linkedAccount = getLinkedAccount(accountToDelete);

       final View checkBoxView = View.inflate(getActivity(), R.layout.delkey_checkbox, null);
       final CheckBox keepAddrCheckbox = checkBoxView.findViewById(R.id.checkbox);
       keepAddrCheckbox.setText(getString(R.string.keep_account_address));
       keepAddrCheckbox.setChecked(false);

       final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getActivity());
       deleteDialog.setTitle(R.string.delete_account_title);
       deleteDialog.setMessage(Html.fromHtml(createDeleteDialogText(accountToDelete, linkedAccount)));

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
               Long satoshis = getPotentialBalance(accountToDelete);
               AlertDialog.Builder confirmDeleteDialog = new AlertDialog.Builder(getActivity());
               confirmDeleteDialog.setTitle(R.string.confirm_delete_pk_title);

               // Set the message. There are four combinations, with and without label, with and without BTC amount.
               String label = _mbwManager.getMetadataStorage().getLabelByAccount(accountToDelete.getId());
               int labelCount = 1;
               if (accountToDelete instanceof ColuAccount) {
                  label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(((ColuAccount) accountToDelete).getLinkedAccount().getId());
                  labelCount++;
               } else if (linkedAccount != null) {
                  label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
                  labelCount++;
               }
               String message;

               // For active accounts we check whether there is money on them before deleting. we don't know if there
               // is money on archived accounts
               String address;
               if (accountToDelete instanceof SingleAddressAccount) {
                  Map<AddressType, Address> addressMap = ((SingleAddressAccount) accountToDelete).getPublicKey().
                          getAllSupportedAddresses(_mbwManager.getNetwork());
                  address = TextUtils.join("\n\n", addressMap.values());
               } else {
                  Optional<Address> receivingAddress = accountToDelete.getReceivingAddress();
                  if (receivingAddress.isPresent()) {
                     address = receivingAddress.get().toMultiLineString();
                  } else {
                     address = "";
                  }
               }
               if (accountToDelete.isActive() && satoshis != null && satoshis > 0) {
                  if (label != null && label.length() != 0) {

                     message = getResources().getQuantityString(R.plurals.confirm_delete_pk_with_balance_with_label,
                             !(accountToDelete instanceof SingleAddressAccount) ? 1 : 0,
                             getResources().getQuantityString(R.plurals.account_label, labelCount, label),
                             address, accountToDelete instanceof ColuAccount ?
                                     Utils.getColuFormattedValueWithUnit(getPotentialBalanceColu(accountToDelete))
                                     : _mbwManager.getBtcValueString(satoshis));
                  } else {
                     message = getResources().getQuantityString(R.plurals.confirm_delete_pk_with_balance,
                             !(accountToDelete instanceof SingleAddressAccount) ? 1 : 0,
                             address, accountToDelete instanceof ColuAccount ?
                                     Utils.getColuFormattedValueWithUnit(getPotentialBalanceColu(accountToDelete))
                                     : _mbwManager.getBtcValueString(satoshis));
                  }
               } else {
                  if (label != null && label.length() != 0) {
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
                           WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, _mbwManager.getColuManager().getAccounts().values());
                           if (linkedColuAccount instanceof ColuAccount) {
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
                                 _storage.deleteAccountMetadata(accountToDelete.getId());
                                 _toaster.toast("Deleting account.", false);
                                 _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                                 eventBus.post(new ExtraAccountsChanged()); // do we need to pass UUID ?
                              }
                           } catch (Exception e) {
                              // make a message !
                              _toaster.toast(getString(R.string.colu_error_deleting), false);
                           }
                        } else {
                           //Check if this SingleAddress account is related with ColuAccount
                           WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, _mbwManager.getColuManager().getAccounts().values());
                           if (linkedColuAccount instanceof ColuAccount) {
                              ColuManager coluManager = _mbwManager.getColuManager();
                              coluManager.deleteAccount((ColuAccount) linkedColuAccount);
                           } else {
                              try {
                                 walletManager.deleteUnrelatedAccount(accountToDelete.getId(), AesKeyCipher.defaultKeyCipher());
                              } catch (KeyCipher.InvalidKeyCipher e) {
                                 throw new RuntimeException(e);
                              }
                           }
                           _storage.deleteAccountMetadata(accountToDelete.getId());
                           _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                           _toaster.toast(R.string.account_deleted, false);
                           eventBus.post(new ExtraAccountsChanged());
                        }
                     }
                     finishCurrentActionMode();
                     eventBus.post(new AccountChanged(accountToDelete.getId()));
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
                  eventBus.post(new ExtraAccountsChanged()); // do we need to pass UUID ?
               } else {
                  //Check if this SingleAddress account is related with ColuAccount
                  WalletAccount linkedColuAccount = Utils.getLinkedAccount(accountToDelete, _mbwManager.getColuManager().getAccounts().values());
                  if (linkedColuAccount != null && linkedColuAccount instanceof ColuAccount) {
                     ColuManager coluManager = _mbwManager.getColuManager();
                     coluManager.deleteAccount((ColuAccount) linkedColuAccount);
                     eventBus.post(new ExtraAccountsChanged()); // do we need to pass UUID ?
                     _storage.deleteAccountMetadata(linkedColuAccount.getId());
                  } else {
                     try {
                        walletManager.deleteUnrelatedAccount(accountToDelete.getId(), AesKeyCipher.defaultKeyCipher());
                     } catch (KeyCipher.InvalidKeyCipher e) {
                        throw new RuntimeException(e);
                     }
                  }
               }
               _storage.deleteAccountMetadata(accountToDelete.getId());
               finishCurrentActionMode();
               eventBus.post(new AccountChanged(accountToDelete.getId()));
               eventBus.post(new ExtraAccountsChanged());
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

   @NonNull
   private String createDeleteDialogText(WalletAccount accountToDelete, WalletAccount linkedAccount) {
      String accountName = _mbwManager.getMetadataStorage().getLabelByAccount(accountToDelete.getId());
      String dialogText;

      if (accountToDelete.isActive()) {
         dialogText = getActiveAccountDeleteText(accountToDelete, linkedAccount, accountName);
      } else {
         dialogText = getArchivedAccountDeleteText(linkedAccount, accountName);
      }
      return dialogText;
   }

   @NonNull
   private String getArchivedAccountDeleteText(WalletAccount linkedAccount, String accountName) {
      String dialogText;
      if (linkedAccount != null && linkedAccount.isVisible()) {
         String linkedAccountName =_mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
         dialogText = getString(R.string.delete_archived_account_message, accountName, linkedAccountName);
      } else {
         dialogText = getString(R.string.delete_archived_account_message_s, accountName);
      }
      return dialogText;
   }

   @NonNull
   private String getActiveAccountDeleteText(WalletAccount accountToDelete, WalletAccount linkedAccount, String accountName) {
      String dialogText;
      CurrencyBasedBalance balance = Preconditions.checkNotNull(accountToDelete.getCurrencyBasedBalance());
      String valueString = getBalanceString(accountToDelete, balance);

      if (linkedAccount != null && linkedAccount.isVisible()) {
         CurrencyBasedBalance linkedBalance = linkedAccount.getCurrencyBasedBalance();
         String linkedValueString = getBalanceString(linkedAccount, linkedBalance);
         String linkedAccountName =_mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
         dialogText = getString(R.string.delete_account_message, accountName, valueString,
                 linkedAccountName, linkedValueString) + "\n" +
                 getString(R.string.both_rmc_will_deleted, accountName, linkedAccountName);
      } else {
         dialogText = getString(R.string.delete_account_message_s, accountName, valueString);
      }
      return dialogText;
   }

   private String getBalanceString(WalletAccount account, CurrencyBasedBalance balance) {
      String valueString = Utils.getFormattedValueWithUnit(balance.confirmed, _mbwManager.getBitcoinDenomination());
      if (account.getType() == WalletAccount.Type.COLU) {
         valueString = Utils.getColuFormattedValueWithUnit(balance.confirmed);
      }
      return valueString;
   }

   /**
    * If account is colu we are asking for linked BTC. Else we are searching if any colu attached.
    */
   private WalletAccount getLinkedAccount(WalletAccount account) {
      WalletAccount linkedAccount;
      if (account.getType() ==  WalletAccount.Type.COLU) {
         linkedAccount = ((ColuAccount) account).getLinkedAccount();
      } else {
         linkedAccount = Utils.getLinkedAccount(account, _mbwManager.getColuManager().getAccounts().values());
      }

      if (linkedAccount == null) {
         linkedAccount = _mbwManager.getWalletManager(false).getAccount(MbwManager.getBitcoinCashAccountId(account));
      }
      return linkedAccount;
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

    private void setNameForUpgradeAccount(WalletAccount account) {
        // special case for sa upgrade accounts
        List<UUID> uuidList = walletManager.getAccountVirtualIds((SingleAddressAccount) account);
        String oldName = "";
        // delete all previous records associated with virtual ids but keep name
        for (UUID uuid : uuidList) {
            if (!_storage.getLabelByAccount(uuid).isEmpty()) {
                oldName = _storage.getLabelByAccount(uuid);
            }
            _storage.deleteAccountMetadata(uuid);
        }
        // store single id with an old name
        _storage.storeAccountLabel(account.getId(), oldName);
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

      if (account.isActive() && account.canSpend() && !(account instanceof HDPubOnlyAccount)
              && !isBch && !(account instanceof HDAccountExternalSignature)) {
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

      if (account.getType() != WalletAccount.Type.BCHBIP44
              && account.getType() != WalletAccount.Type.BCHSINGLEADDRESS
              && account.isArchived()) {
         menus.add(R.menu.record_options_menu_archive);
      }

      if (account.isActive() && account instanceof ExportableAccount && !isBch) {
         menus.add(R.menu.record_options_menu_export);
      }

      if (account.isActive() && account instanceof HDAccount && !(account instanceof HDPubOnlyAccount)
              && AccountManager.INSTANCE.getBTCMasterSeedAccounts().size() > 1 && !isBch) {

         final HDAccount HDAccount = (HDAccount) account;
         if (!HDAccount.hasHadActivity() && HDAccount.getAccountIndex() == walletManager.getCurrentBip44Index()) {
            //only allow to remove unused HD acounts from the view
            menus.add(R.menu.record_options_menu_hide_unused);
         }
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
            switch (id) {
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
               case R.id.miRescan:
                  rescan();
                  return true;
               case R.id.miSetMail:
                  setCoinapultMail();
                  return true;
               case R.id.miVerifyMail:
                  verifyCoinapultMail();
                  return true;
               default:
                  return false;
            }
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
      _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
            if (_focusedAccount instanceof CoinapultAccount) {
               CoinapultManager coinapultManager = _mbwManager.getCoinapultManager();
               MessageSigningActivity.callMe(getActivity(), coinapultManager.getAccountKey(), AddressType.P2SH_P2WPKH);
            } else if (_focusedAccount instanceof SingleAddressAccount) {
               MessageSigningActivity.callMe(getActivity(), (SingleAddressAccount) _focusedAccount,
                       ((SingleAddressAccount) _focusedAccount).getAddress().getType());
            } else if(_focusedAccount instanceof ColuAccount){
               MessageSigningActivity.callMe(getActivity(), ((ColuAccount) _focusedAccount).getPrivateKey(),
                       AddressType.P2PKH);
            } else {
               Intent intent = new Intent(getActivity(), HDSigningActivity.class)
                       .putExtra("account", _focusedAccount.getId());
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
      } else if (account instanceof HDAccount) {
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
      if (!isAdded()) {
         return true;
      }
      if (item.getItemId() == R.id.miAddRecord) {
         AddAccountActivity.callMe(this, ADD_RECORD_RESULT_CODE);
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
         _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

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
      if (_focusedAccount.isActive() && accountProtected(_focusedAccount)) {
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
      _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

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
      _mbwManager.getColuManager().startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED);
   }

   private void exportSelectedPrivateKey() {
      if (!isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

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
      _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

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
      _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

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
      WalletAccount correspondingBCHAccount = _mbwManager.getWalletManager(false).getAccount(MbwManager.getBitcoinCashAccountId(account));
      if (correspondingBCHAccount != null) {
         correspondingBCHAccount.activateAccount();
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
      final WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if (accountProtected(_focusedAccount)) {
         //this is the last active hd account, we dont allow archiving it
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
      if (_focusedAccount.getType() == WalletAccount.Type.COINAPULT) {
         _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

            @Override
            public void run() {
               if (!AccountsFragment.this.isAdded()) {
                  return;
               }

               archive(_focusedAccount);
            }

         });
         return;
      } else if (_focusedAccount.getType() == WalletAccount.Type.BTCBIP44) {
         HDAccount account = (HDAccount) _focusedAccount;
         if (!account.hasHadActivity()) {
            //this account is unused, we dont allow archiving it
            _toaster.toast(R.string.dont_allow_archiving_unused_notification, false);
            return;
         }
      }
      _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }

            archive(_focusedAccount);
         }

      });
   }

   /**
    * Account is protected if after removal no BTC masterseed accounts would stay active, so it would not be possible to select an account
    */
   private boolean accountProtected(WalletAccount toRemove) {
      if (toRemove.getType() != WalletAccount.Type.BTCBIP44
              || ((HDAccount) toRemove).getAccountType() != HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED) {
         // unprotected account type
         return false;
      }
      Set<WalletAccount> uniqueAccountsSet = new ArraySet<>();
      for (WalletAccount account : _mbwManager.getWalletManager(false).
              getActiveAccounts(WalletAccount.Type.BTCBIP44)) {
         if (((HDAccount) account).getAccountType() == HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED) {
            uniqueAccountsSet.add(account);
         }
         if (uniqueAccountsSet.size() > 1) {
            // after deleting one, more remain
            return false;
         }
      }
      return true;
   }

   private void hideSelected() {
      if (!isAdded()) {
         return;
      }
      final WalletAccount _focusedAccount = accountListAdapter.getFocusedAccount();
      if (accountProtected(_focusedAccount)) {
         //this is the last active account, we dont allow hiding it
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
      if (_focusedAccount instanceof HDAccount) {
         final HDAccount account = (HDAccount) _focusedAccount;
         if (account.hasHadActivity()) {
            //this account is used, we don't allow hiding it
            _toaster.toast(R.string.dont_allow_hiding_used_notification, false);
            return;
         }

         _mbwManager.runPinProtectedFunction(getActivity(), new Runnable() {
            @Override
            public void run() {
               _mbwManager.getWalletManager(false).removeUnusedBip44Account(account);
               //in case user had labeled the account, delete the stored name
               _storage.deleteAccountMetadata(account.getId());
               eventBus.post(new AccountChanged(account.getId()));
               _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
               //we dont want to show the context menu for the automatically selected account
               accountListAdapter.setFocusedAccountId(null);
               finishCurrentActionMode();
            }
         });
      }
   }

   private void archive(final WalletAccount account) {
      CurrencyBasedBalance balance = Preconditions.checkNotNull(account.getCurrencyBasedBalance());
      final WalletAccount linkedAccount = getLinkedAccount(account);
      new AlertDialog.Builder(getActivity())
              .setTitle(R.string.archiving_account_title)
              .setMessage(Html.fromHtml(createArchiveDialogText(account,linkedAccount)))
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                 public void onClick(DialogInterface arg0, int arg1) {
                    account.archiveAccount();
                    WalletAccount linkedAccount = Utils.getLinkedAccount(account, _mbwManager.getColuManager().getAccounts().values());
                    if (linkedAccount != null) {
                       linkedAccount.archiveAccount();
                    }
                    WalletAccount correspondingBCHAccount = _mbwManager.getWalletManager(false).getAccount(MbwManager.getBitcoinCashAccountId(account));
                    if (correspondingBCHAccount != null) {
                       correspondingBCHAccount.archiveAccount();
                    }
                    _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                    eventBus.post(new AccountChanged(account.getId()));
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

   @NonNull
   private String createArchiveDialogText(WalletAccount account, WalletAccount linkedAccount) {
      String accountName = _mbwManager.getMetadataStorage().getLabelByAccount(account.getId());
      return getAccountArchiveText(account, linkedAccount, accountName);
   }

   @NonNull
   private String getAccountArchiveText(WalletAccount account, WalletAccount linkedAccount, String accountName) {
      String dialogText;
      CurrencyBasedBalance balance = Preconditions.checkNotNull(account.getCurrencyBasedBalance());
      String valueString = getBalanceString(account, balance);

      if (linkedAccount != null && linkedAccount.isVisible()) {
         CurrencyBasedBalance linkedBalance = linkedAccount.getCurrencyBasedBalance();
         String linkedValueString = getBalanceString(linkedAccount, linkedBalance);
         String linkedAccountName =_mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
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

   private LocalTraderEventSubscriber ltSubscriber = new LocalTraderEventSubscriber(new Handler()) {
      @Override
      public void onLtError(int errorCode) { }

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
