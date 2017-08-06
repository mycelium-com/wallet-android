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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.AddAccountActivity;
import com.mycelium.wallet.activity.AddCoinapultAccountActivity;
import com.mycelium.wallet.activity.AddColuAccountActivity;
import com.mycelium.wallet.activity.MessageSigningActivity;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.util.ToggleableCurrencyDisplay;
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
import com.mycelium.wapi.wallet.currency.CurrencySum;
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
   private LayoutInflater _layoutInflater;
   private int _separatorColor;
   private LayoutParams _separatorLayoutParameters;
   private LayoutParams _outerLayoutParameters;
   private LayoutParams _innerLayoutParameters;
   private WalletAccount _focusedAccount;
   private Toaster _toaster;
   private ProgressDialog _progress;

   /**
    * Called when the activity is first created.
    */
   @SuppressWarnings("deprecation")
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = inflater.inflate(R.layout.records_activity, container, false);
      _layoutInflater = inflater;

      _separatorColor = getResources().getColor(R.color.darkgrey);
      _separatorLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, getDipValue(1), 1);
      _outerLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      _outerLayoutParameters.bottomMargin = getDipValue(8);
      _innerLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      return ret;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
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

   private int getDipValue(int dip) {
      return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
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
         _focusedAccount = account;
         update();
         return;
      }

        if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            UUID accountid = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
            //check whether the account is active - we might have scanned the priv key for an archived watchonly
            WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountid);
            if (account.isActive()) {
                _mbwManager.setSelectedAccount(accountid);
            }
            _focusedAccount = account;
            update();
            setNameForNewAccount(_focusedAccount);
        } else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    // TODO: refactor these RESULT_XXX constants in a common class ?

      if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == AddColuAccountActivity.RESULT_COLU) {
         UUID accountId = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
         ColuAccount account = (ColuAccount) _mbwManager.getWalletManager(false).getAccount(accountId);
         _mbwManager.setSelectedAccount(accountId);
         _focusedAccount = account;
         update();
         return;
      }

      if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         UUID accountid = (UUID) intent.getSerializableExtra(AddAccountActivity.RESULT_KEY);
         //check whether the account is active - we might have scanned the priv key for an archived watchonly
         WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountid);
         if (account.isActive()) {
            _mbwManager.setSelectedAccount(accountid);
         }
         _focusedAccount = account;
         update();
         setNameForNewAccount(_focusedAccount);
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
      final boolean hasPrivateData = (accountToDelete instanceof ColuAccount) ||
            (accountToDelete instanceof ExportableAccount
            && ((ExportableAccount) accountToDelete).getExportData(AesKeyCipher.defaultKeyCipher()).privateData.isPresent());

      if (accountToDelete instanceof SingleAddressAccount && hasPrivateData) {
         deleteDialog.setView(checkBoxView);
      }

      if (accountToDelete instanceof ColuAccount) {
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
               if (account instanceof ColuAccount) {
                  label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(((ColuAccount) account).getLinkedAccount().getId());
               } else if (linkedAccount != null) {
                  label += ", " + _mbwManager.getMetadataStorage().getLabelByAccount(linkedAccount.getId());
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
                     message = getString(R.string.confirm_delete_pk_with_balance_with_label, label,
                                address, accountToDelete instanceof ColuAccount ?
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
                     message = getString(
                           R.string.confirm_delete_pk_without_balance_with_label,
                           label,
                           receivingAddress.isPresent() ? receivingAddress.get().toMultiLineString() : ""
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
                     } else if(accountToDelete instanceof ColuAccount) {
                        try {
                           ColuManager coluManager = _mbwManager.getColuManager();
                           if (keepAddrCheckbox.isChecked()) {
                              coluManager.forgetPrivateKey((ColuAccount) accountToDelete);
                           } else {
                              coluManager.deleteAccount((ColuAccount) accountToDelete);
                              _toaster.toast("Deleting account", false);
                              _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
                              _mbwManager.getEventBus().post(new ExtraAccountsChanged()); // do we need to pass UUID ?
                           }
                        } catch(Exception e) {
                            // make a message !
                            _toaster.toast("Got an error while deleting colu account", false);
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
               ExchangeRateManager exchanger = _mbwManager.getExchangeRateManager();
               return balance.confirmed.add(balance.receiving, exchanger);
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
      LinearLayout llRecords = (LinearLayout) getView().findViewById(R.id.llRecords);
      llRecords.removeAllViews();

      if (_mbwManager.isKeyManagementLocked()) {
         // Key management is locked
         getView().findViewById(R.id.svRecords).setVisibility(View.GONE);
         getView().findViewById(R.id.llLocked).setVisibility(View.VISIBLE);
      } else {
         // Make all the key management functionality available to experts
         getView().findViewById(R.id.svRecords).setVisibility(View.VISIBLE);
         getView().findViewById(R.id.llLocked).setVisibility(View.GONE);


         List<WalletAccount> activeHdAccounts = walletManager.getActiveMasterseedAccounts();
         List<WalletAccount> activeOtherAccounts = walletManager.getActiveOtherAccounts();

         List<WalletAccount> activeHdRecords = Utils.sortAccounts(activeHdAccounts, _storage);
         List<WalletAccount> activeOtherRecords = Utils.sortAccounts(activeOtherAccounts, _storage);
         List<WalletAccount> archivedRecords = Utils.sortAccounts(walletManager.getArchivedAccounts(), _storage);

         WalletAccount selectedAccount = _mbwManager.getSelectedAccount();

         CurrencySum totalSpendableBalance = new CurrencySum();
         String activeTitle = getString(R.string.active_hd_accounts_name) + (activeHdRecords.isEmpty() ? " " + getString(R.string.active_accounts_empty) : "");
         CurrencySum spendableBalanceHdAccounts = getSpendableBalance(activeHdAccounts);
         LinearLayout activeHdAccountsView = createAccountViewList(activeTitle, activeHdRecords, selectedAccount, spendableBalanceHdAccounts);
         llRecords.addView(activeHdAccountsView);

         totalSpendableBalance.add(spendableBalanceHdAccounts);

         if (!activeOtherRecords.isEmpty()) {
            CurrencySum spendableBalanceOtherAccounts = getSpendableBalance(activeOtherAccounts);
            LinearLayout activeOtherAccountsView = createAccountViewList(getString(R.string.active_other_accounts_name), activeOtherRecords, selectedAccount, spendableBalanceOtherAccounts);
            llRecords.addView(activeOtherAccountsView);

            totalSpendableBalance.add(spendableBalanceOtherAccounts);

            // only show a totals row, if both account type exits
            LinearLayout activeOtherSum = createActiveAccountBalanceSumView(totalSpendableBalance);
            llRecords.addView(activeOtherSum);
         }

         if (archivedRecords.size() > 0) {
            LinearLayout archived = createAccountViewList(getString(R.string.archive_name), archivedRecords, selectedAccount, null);
            llRecords.addView(archived);
         }
      }
   }

   private LinearLayout createActiveAccountBalanceSumView(CurrencySum spendableBalance) {
      LinearLayout outer = new LinearLayout(getActivity());
      outer.setOrientation(LinearLayout.VERTICAL);
      outer.setLayoutParams(_outerLayoutParameters);

      LinearLayout inner = new LinearLayout(getActivity());
      inner.setOrientation(LinearLayout.VERTICAL);
      inner.setLayoutParams(_innerLayoutParameters);
      inner.requestLayout();

      // Add records
      RecordRowBuilder builder = new RecordRowBuilder(_mbwManager, getResources(), _layoutInflater);

      // Add item
      View item = builder.buildTotalView(outer, spendableBalance);
      inner.addView(item);

      // Add separator
      inner.addView(createSeparator());

      outer.addView(inner);
      return outer;
   }

   private CurrencySum getSpendableBalance(List<WalletAccount> accounts) {
      CurrencySum currencySum = new CurrencySum();
      for (WalletAccount account : accounts) {
         currencySum.add(account.getCurrencyBasedBalance().confirmed);
      }
      return currencySum;
   }

   private LinearLayout createAccountViewList(String title, List<WalletAccount> accounts, WalletAccount selectedAccount, CurrencySum spendableBalance) {
      LinearLayout outer = new LinearLayout(getActivity());
      outer.setOrientation(LinearLayout.VERTICAL);
      outer.setLayoutParams(_outerLayoutParameters);

      // Add title
      createTitle(outer, title, spendableBalance);

      if (accounts.isEmpty()) {
         return outer;
      }

      LinearLayout inner = new LinearLayout(getActivity());
      inner.setOrientation(LinearLayout.VERTICAL);
      inner.setLayoutParams(_innerLayoutParameters);
      inner.requestLayout();

//      // Add records
      RecordRowBuilder builder = new RecordRowBuilder(_mbwManager, getResources(), _layoutInflater);
      for (WalletAccount account : accounts) {
         // Add separator
         inner.addView(createSeparator());

         // Add item
         boolean isSelected = account.equals(selectedAccount);
         View item = createAccountView(outer, account, isSelected, builder);
         inner.addView(item);
      }

      if (accounts.size() > 0) {
         // Add separator
         inner.addView(createSeparator());
      }

      outer.addView(inner);
      return outer;
   }

   private TextView createTitle(ViewGroup root, String title, CurrencySum balance) {
      View view = _layoutInflater.inflate(R.layout.accounts_title_view, root, true);
      TextView tvTitle = (TextView) view.findViewById(R.id.tvTitle);
      tvTitle.setText(title);

      ToggleableCurrencyDisplay tvBalance = (ToggleableCurrencyDisplay) view.findViewById(R.id.tvBalance);
      if (balance != null) {
         tvBalance.setEventBus(_mbwManager.getEventBus());
         tvBalance.setCurrencySwitcher(_mbwManager.getCurrencySwitcher());
         tvBalance.setValue(balance);
      } else {
         tvBalance.setVisibility(View.GONE);
      }

      return tvTitle;
   }

   private View createSeparator() {
      View v = new View(getActivity());
      v.setLayoutParams(_separatorLayoutParameters);
      v.setBackgroundColor(_separatorColor);
      v.setPadding(10, 0, 10, 0);
      return v;
   }

   private View createAccountView(LinearLayout parent, WalletAccount account, boolean isSelected, RecordRowBuilder recordRowBuilder) {
      boolean hasFocus = _focusedAccount != null && account.equals(_focusedAccount);
      View rowView = recordRowBuilder.buildRecordView(parent, account, isSelected, hasFocus);
      rowView.setOnClickListener(recordStarClickListener);
      rowView.findViewById(R.id.llAddress).setOnClickListener(recordAddressClickListener);
      return rowView;
   }

   private OnClickListener recordStarClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         _focusedAccount = (WalletAccount) v.getTag();
         if (_focusedAccount.isActive()) {
            _mbwManager.setSelectedAccount(_focusedAccount.getId());
         }
         update();
      }
   };

   private ActionMode currentActionMode;

   private OnClickListener recordAddressClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         final WalletAccount account = (WalletAccount) ((View) Preconditions.checkNotNull(v.getParent())).getTag();

         // Check whether a new account was selected
         if (!_mbwManager.getSelectedAccount().equals(account) && account.isActive()) {
            _mbwManager.setSelectedAccount(account.getId());
         }
         _focusedAccount = account;
         toastSelectedAccountChanged(account);
         updateIncludingMenus();
      }

   };

   private void updateIncludingMenus() {
      WalletAccount account = _focusedAccount;

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

      if (!account.isDerivedFromInternalMasterseed()) {
         menus.add(R.menu.record_options_menu_delete);
      }

      if (account.isActive() && account.canSpend() && !(account instanceof Bip44PubOnlyAccount)
              && !(account instanceof ColuAccount)
              && !(Utils.checkIsLinked(account, _mbwManager.getColuManager().getAccounts().values()))) {
         menus.add(R.menu.record_options_menu_sign);
      }

      if (account.isActive() && !(account instanceof ColuAccount)
              && !(Utils.checkIsLinked(account, _mbwManager.getColuManager().getAccounts().values()))) {
         menus.add(R.menu.record_options_menu_active);
      }

      if (account.isActive() && !(account instanceof CoinapultAccount)) {
         menus.add(R.menu.record_options_menu_outputs);
      }

      if (account instanceof CoinapultAccount) {
         menus.add(R.menu.record_options_menu_set_coinapult_mail);
      }

      if (account.isArchived()) {
         menus.add(R.menu.record_options_menu_archive);
      }

      if (account.isActive() && account instanceof ExportableAccount) {
         menus.add(R.menu.record_options_menu_export);
      }

      if (account.isActive() && account instanceof Bip44Account && !(account instanceof Bip44PubOnlyAccount)) {
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

      ActionBarActivity parent = (ActionBarActivity) getActivity();

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
               setLabelOnAccount(_focusedAccount, "", true);
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
            if (_focusedAccount != null) {
               _focusedAccount = null;
               update();
            }
         }
      };
      currentActionMode = parent.startSupportActionMode(actionMode);
      // Late set the focused record. We have to do this after
      // startSupportActionMode above, as it calls onDestroyActionMode when
      // starting for some reason, and this would clear the focus and force
      // an update.
      _focusedAccount = account;

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

      if (_focusedAccount instanceof SingleAddressAccount || _focusedAccount instanceof ColuAccount) {
         //start legacy backup verification
         VerifyBackupActivity.callMe(getActivity());
      }
   }

   private void makeBackup() {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }

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
            if (_focusedAccount instanceof CoinapultAccount) {
               CoinapultManager coinapultManager = _mbwManager.getCoinapultManager();
               MessageSigningActivity.callMe(getActivity(), coinapultManager.getAccountKey());
            } else if (_focusedAccount instanceof SingleAddressAccount) {
               MessageSigningActivity.callMe(getActivity(), (SingleAddressAccount) _focusedAccount);
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
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      _focusedAccount.dropCachedData();
      _mbwManager.getWalletManager(false).startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED);
   }

   private void ignoreSelectedPrivateKey() {
      if (!AccountsFragment.this.isAdded()) {
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
      if (!AccountsFragment.this.isAdded()) {
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
      if (!AccountsFragment.this.isAdded()) {
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
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(AccountsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!AccountsFragment.this.isAdded()) {
               return;
            }
            activate(_focusedAccount);
         }

      });
   }

   private void activate(WalletAccount account) {
      account.activateAccount();
      //setselected also broadcasts AccountChanged event
      _mbwManager.setSelectedAccount(account.getId());
      updateIncludingMenus();
      _toaster.toast(R.string.activated, false);
      _mbwManager.getWalletManager(false).startSynchronization();
   }

   private void archiveSelected() {
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      if (_mbwManager.getWalletManager(false).getActiveAccounts().size() < 2) {
         //this is the last active account, we dont allow archiving it
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
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
      if (!AccountsFragment.this.isAdded()) {
         return;
      }
      if (_mbwManager.getWalletManager(false).getActiveAccounts().size() < 2) {
         //this is the last active account, we dont allow hiding it
         _toaster.toast(R.string.keep_one_active, false);
         return;
      }
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
               _mbwManager.getWalletManager(false).removeUnusedBip44Account();
               //in case user had labeled the account, delete the stored name
               _storage.deleteAccountMetadata(account.getId());
               //setselected also broadcasts AccountChanged event, which will cause an ui update
               _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
               //we dont want to show the context menu for the automatically selected account
               _focusedAccount = null;
               finishCurrentActionMode();
            }
         });

      }
   }

   private void archive(final WalletAccount account) {
      AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
      confirmDialog.setTitle(R.string.archiving_account_title);
      confirmDialog.setMessage(getString(R.string.question_archive_account));
      confirmDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
            account.archiveAccount();
            _mbwManager.setSelectedAccount(_mbwManager.getWalletManager(false).getActiveAccounts().get(0).getId());
            _mbwManager.getEventBus().post(new AccountChanged(account.getId()));
            updateIncludingMenus();
            _toaster.toast(R.string.archived, false);
         }
      });
      confirmDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
         }
      });
      confirmDialog.show();
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
   public void syncStarting(SyncStopped event) {
      update();
   }

   @Subscribe
   public void accountChanged(AccountChanged event) {
      update();
   }

}
