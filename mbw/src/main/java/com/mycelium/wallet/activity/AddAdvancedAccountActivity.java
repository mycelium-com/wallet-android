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

import static com.mycelium.wallet.activity.BipSsImportActivity.RESULT_SECRET;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAddress;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getAssetUri;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getHdKeyNode;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getPrivateKey;
import static com.mycelium.wallet.activity.util.IntentExtentionsKt.getShare;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.BipSss;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.util.ImportCoCoHDAccount;
import com.mycelium.wallet.activity.util.ValueExtensionsKt;
import com.mycelium.wallet.content.HandleConfigFactory;
import com.mycelium.wallet.content.ResultType;
import com.mycelium.wallet.event.AccountChanged;
import com.mycelium.wallet.event.AccountCreated;
import com.mycelium.wallet.external.partner.PartnerExtKt;
import com.mycelium.wallet.extsig.keepkey.activity.KeepKeyAccountImportActivity;
import com.mycelium.wallet.extsig.ledger.activity.LedgerAccountImportActivity;
import com.mycelium.wallet.extsig.trezor.activity.TrezorAccountImportActivity;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig;
import com.mycelium.wapi.wallet.btc.single.AddressSingleConfig;
import com.mycelium.wapi.wallet.btc.single.PrivateSingleConfig;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.eth.EthAddress;
import com.mycelium.wapi.wallet.eth.EthAddressConfig;
import com.mycelium.wapi.wallet.eth.coins.EthCoin;
import com.mycelium.wapi.wallet.fio.FIOAddressConfig;
import com.mycelium.wapi.wallet.fio.FIOUnrelatedHDConfig;
import com.mycelium.wapi.wallet.fio.FioAddress;
import com.mycelium.wapi.wallet.fio.FioKeyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddAdvancedAccountActivity extends AppCompatActivity implements ImportCoCoHDAccount.FinishListener {
   public static final int RESULT_MSG = 25;

   public static void callMe(Activity activity, int requestCode) {
      Intent intent = new Intent(activity, AddAdvancedAccountActivity.class);
      activity.startActivityForResult(intent, requestCode);
   }

   private static final int SCAN_RESULT_CODE = 0;
   private static final int CREATE_RESULT_CODE = 1;
   private static final int TREZOR_RESULT_CODE = 2;
   private static final int CLIPBOARD_RESULT_CODE = 3;
   private static final int LEDGER_RESULT_CODE = 4;
   private static final int KEEPKEY_RESULT_CODE = 5;
   private MbwManager _mbwManager;

   private NetworkParameters _network;

   @BindView(R.id.btGenerateNewBchSingleKey)
   View btGenerateNewBchSingleKey;

   @BindView(R.id.btCreateFioLegacyAccount)
   View btCreateFioLegacyAccount;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.add_advanced_account_activity);
      ButterKnife.bind(this);
      getSupportActionBar().setTitle(R.string.add_unrelated_account_title);
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      final Activity activity = AddAdvancedAccountActivity.this;
      _mbwManager = MbwManager.getInstance(this);
      _network = _mbwManager.getNetwork();

      findViewById(R.id.btScan).setOnClickListener(v -> ScanActivity.callMe(activity, SCAN_RESULT_CODE, HandleConfigFactory.returnKeyOrAddressOrHdNode()));

      findViewById(R.id.btGenerateNewSingleKey).setOnClickListener(view -> {
         Intent intent = new Intent(activity, CreateKeyActivity.class);
         startActivityForResult(intent, CREATE_RESULT_CODE);
      });

      findViewById(R.id.btTrezor).setOnClickListener(view -> TrezorAccountImportActivity.callMe(activity, TREZOR_RESULT_CODE));

      findViewById(R.id.btKeepKey).setOnClickListener(view -> KeepKeyAccountImportActivity.callMe(activity, KEEPKEY_RESULT_CODE));

      findViewById(R.id.btLedger).setOnClickListener(view -> LedgerAccountImportActivity.callMe(activity, LEDGER_RESULT_CODE));

      btGenerateNewBchSingleKey.setVisibility(View.GONE);

      btCreateFioLegacyAccount.setOnClickListener(view -> {
         FioKeyManager fioKeyManager = _mbwManager.fioKeyManager;
         HdKeyNode legacyFioNode = fioKeyManager.getLegacyFioNode();
         //since uuid is used for account creation we can check hdKeynode uuid for account existence
         if (_mbwManager.getWalletManager(false).getAccount(legacyFioNode.getUuid()) == null) {
            ArrayList<HdKeyNode> nodes = new ArrayList<HdKeyNode>() {{
               add(legacyFioNode);
            }};
            List<UUID> account = _mbwManager.getWalletManager(false).createAccounts(new FIOUnrelatedHDConfig(nodes, getString(R.string.base_label_fio_account_legacy)));
            finishOk(account.get(0), false);
         } else {
            new Toaster(this).toast(R.string.fio_legacy_already_created, false);
         }
      });
   }

   @Override
   public void onResume() {
      super.onResume();

      StringHandlerActivity.ParseAbility canHandle = StringHandlerActivity.canHandle (
              HandleConfigFactory.returnKeyOrAddressOrHdNode(),
              Utils.getClipboardString(AddAdvancedAccountActivity.this),
              MbwManager.getInstance(this).getNetwork());

      boolean canImportFromClipboard = (canHandle != StringHandlerActivity.ParseAbility.NO);

      Button clip = findViewById(R.id.btClipboard);
      clip.setEnabled(canImportFromClipboard);
      if (canImportFromClipboard) {
         clip.setText(R.string.clipboard);
      } else {
         clip.setText(R.string.clipboard_not_available);
      }
      clip.setOnClickListener(v -> {
         Intent intent = StringHandlerActivity.getIntent(AddAdvancedAccountActivity.this,
                 HandleConfigFactory.returnKeyOrAddressOrHdNode(),
                 Utils.getClipboardString(AddAdvancedAccountActivity.this));

         startActivityForResult(intent, CLIPBOARD_RESULT_CODE);
      });
   }

   /**
    * SA watch only accounts import method.
    */
   private void returnAccount(Address address) {
      // temporary solution: unrelated Ethereum accounts will be implemented later
      if (address.getCoinType() instanceof EthCoin) {
         new Toaster(this).toast("Importing unrelated Ethereum accounts still to be implemented.", false);
         return;
      }

      new ImportReadOnlySingleAddressAccountAsyncTask(address).execute();
   }

   /**
    * BIP44 account import method.
    * @param hdKeyNode node of depth 3.
    */
   private void returnAccount(HdKeyNode hdKeyNode) {
      UUID acc = _mbwManager.getWalletManager(false)
              .createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKeyNode))).get(0);
      // set BackupState as ignored - we currently have no option to backup xPrivs after all
      _mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);
      finishOk(acc, false);
   }

   /**
    *  This method is only intended to support BIP32 CoCo accounts.
    * @param hdKeyNode node of depth 0
    */
   private void returnBip32Account(final HdKeyNode hdKeyNode) {
      if (hdKeyNode.getDepth() != 0) {
         throw new IllegalArgumentException("Only nodes of depth 0 are supported");
      }
      if (isNetworkActive()) {
         createAskForScanDialog(hdKeyNode);
      } else {
         createAskForNetworkDialog(hdKeyNode);
      }
   }

   private void createAskForNetworkDialog(final HdKeyNode hdKeyNode) {
      new AlertDialog.Builder(this)
              .setTitle(R.string.coco_service_unavailable)
              .setMessage(R.string.connection_unavailable)
              .setCancelable(true)
              .setPositiveButton(R.string.try_again, (dialog, id) -> returnBip32Account(hdKeyNode))
              .setNegativeButton(R.string.cancel, null)
              .create()
              .show();
   }

   private void createAskForScanDialog(final HdKeyNode hdKeyNode) {
      new AlertDialog.Builder(this)
              .setTitle(R.string.attention)
              .setMessage(R.string.coco_scan_warning)
              .setCancelable(true)
              .setPositiveButton(R.string.button_continue, (dialog, id) -> {
                 ImportCoCoHDAccount importCoCoHDAccount = new ImportCoCoHDAccount(AddAdvancedAccountActivity.this, hdKeyNode);
                 importCoCoHDAccount.setFinishListener(AddAdvancedAccountActivity.this);
                 importCoCoHDAccount.execute();
              })
              .setNegativeButton(R.string.cancel, null)
              .create()
              .show();
   }

   private boolean isNetworkActive() {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
      return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case android.R.id.home:
            onBackPressed();
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCAN_RESULT_CODE || requestCode == CLIPBOARD_RESULT_CODE) {
         if (resultCode == Activity.RESULT_OK) {
            boolean fromClipboard = (requestCode == CLIPBOARD_RESULT_CODE);

            ResultType type = (ResultType) intent.getSerializableExtra(StringHandlerActivity.RESULT_TYPE_KEY);
            switch (type) {
               case PRIVATE_KEY:
                  InMemoryPrivateKey key = getPrivateKey(intent);
                  if (fromClipboard) {
                     Utils.clearClipboardString(AddAdvancedAccountActivity.this);
                  }

                  // We imported this key from somewhere else - so we guess, that there exists an backup
                  returnAccount(key, MetadataStorage.BackupState.IGNORED, AccountType.Unknown);
                  break;
               case ADDRESS:
                  returnAccount(getAddress(intent));
                  break;
               case HD_NODE:
                  final HdKeyNode hdKeyNode = getHdKeyNode(intent);
                  if (fromClipboard && hdKeyNode.isPrivateHdKeyNode()) {
                     Utils.clearClipboardString(AddAdvancedAccountActivity.this);
                  }
                  processNode(hdKeyNode);
                  break;
               case ASSET_URI:
                  // uri result must be with address, can check request HandleConfigFactory.returnKeyOrAddressOrHdNode
                  returnAccount(getAssetUri(intent).getAddress());
                  break;
               case SHARE:
                  BipSss.Share share = getShare(intent);
                  BipSsImportActivity.callMe(this, share, StringHandlerActivity.IMPORT_SSS_CONTENT_CODE);
                  break;
               default:
                  throw new IllegalStateException("Unexpected result type from scan: " + type.toString());
            }
         } else {
            ScanActivity.toastScanError(resultCode, intent, this);
         }
      } else if (requestCode == CREATE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         String base58Key = intent.getStringExtra("base58key");
         InMemoryPrivateKey key = InMemoryPrivateKey.fromBase58String(base58Key, _network);
         if (key != null) {
            // This is a new key - there is no existing backup
            returnAccount(key, MetadataStorage.BackupState.UNKNOWN, AccountType.SA);
         } else {
            throw new RuntimeException("Creating private key from string unexpectedly failed.");
         }
      } else if (requestCode == TREZOR_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         // already added to the WalletManager - just return the new account
         finishOk((UUID) intent.getSerializableExtra("account"), false);
      } else if (requestCode == KEEPKEY_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         // already added to the WalletManager - just return the new account
         finishOk((UUID) intent.getSerializableExtra("account"), false);
      } else if (requestCode == LEDGER_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         // already added to the WalletManager - just return the new account
         finishOk((UUID) intent.getSerializableExtra("account"), false);
      } else if (requestCode == StringHandlerActivity.IMPORT_SSS_CONTENT_CODE && resultCode == Activity.RESULT_OK) {
         String base58Key = intent.getStringExtra(RESULT_SECRET);
         Optional<InMemoryPrivateKey> key = InMemoryPrivateKey.fromBase58String(base58Key, _network);
         returnAccount(key.get(), MetadataStorage.BackupState.IGNORED, AccountType.Unknown);
         //
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void processNode(final HdKeyNode hdKeyNode) {
      int depth = hdKeyNode.getDepth();
      switch (depth) {
         case 3:
            if (_mbwManager.getWalletManager(false).hasAccount(hdKeyNode.getUuid())){
               final WalletAccount existingAccount = _mbwManager.getWalletManager(false).getAccount(hdKeyNode.getUuid());
               if (hdKeyNode.isPrivateHdKeyNode() && !existingAccount.canSpend()) {
                  new AlertDialog.Builder(AddAdvancedAccountActivity.this)
                          .setTitle(R.string.priv_key_of_watch_only_account)
                          .setMessage(getString(R.string.want_to_add_priv_key_to_watch_account, _mbwManager.getMetadataStorage().getLabelByAccount(hdKeyNode.getUuid())))
                          .setNegativeButton(R.string.cancel, (dialogInterface, i) -> finishAlreadyExist(existingAccount.getReceiveAddress()))
                          .setPositiveButton(R.string.ok, (dialogInterface, i) -> returnAccount(hdKeyNode, true))
                          .create()
                          .show();
               }
            } else {
               returnAccount(hdKeyNode);
            }
            break;
         case 0:
            // This branch is created to support import CoCo from bip32 account
            if (hdKeyNode.isPrivateHdKeyNode()) {
               returnBip32Account(hdKeyNode);
            } else {
               new Toaster(this).toast(getString(R.string.import_xpub_should_xpriv), false);
            }
            break;
         default:
            String errorMessage = getString(R.string.import_xpub_wrong_depth, Integer.toString(depth));
            new Toaster(this).toast(errorMessage, false);
      }
   }

   // restore single account in asynctask so we can handle Colored Coins case
   private class ImportSingleAddressAccountAsyncTask extends AsyncTask<Void, Integer, AddressCheckResult> {
      private InMemoryPrivateKey key;
      private MetadataStorage.BackupState backupState;
      private List<WalletAccount<?>> existingAccounts = new ArrayList<>();

      ImportSingleAddressAccountAsyncTask(InMemoryPrivateKey key, MetadataStorage.BackupState backupState) {
         this.key = key;
         this.backupState = backupState;
      }

      @Override
      protected AddressCheckResult doInBackground(Void... params) {
         WalletManager walletManager = _mbwManager.getWalletManager(false);
         //Check whether this address is already used in any account
         for (BitcoinAddress addr : key.getPublicKey().getAllSupportedAddresses(_mbwManager.getNetwork()).values()) {
            Address checkedAddress = AddressUtils.fromAddress(addr);
            List<WalletAccount<?>> accounts = walletManager.getAccountsBy(checkedAddress);
            if (!accounts.isEmpty()) {
               existingAccounts = accounts;
               break;
            }
         }
         return AddressCheckResult.BTC;
      }

      @Override
      protected void onPostExecute(AddressCheckResult result) {
         if (existingAccounts.isEmpty()) {
            UUID account1 = returnSAAccount(key, backupState);
            finishOk(account1, false);
         } else {
            WalletAccount accountToUpgrade = null;
            WalletAccount existingAccount = existingAccounts.get(0);

            if (!existingAccount.canSpend()) {
               accountToUpgrade = existingAccount;
            } else {
               finishAlreadyExist(existingAccount.getReceiveAddress());
            }

            if (accountToUpgrade != null) {
               doUpgrade(accountToUpgrade, key);
            }
         }
      }
   }

   void doUpgrade(WalletAccount accountToUpgrade, InMemoryPrivateKey privateKey) {
      final InMemoryPrivateKey key = privateKey;
      final WalletAccount accToUpgrade = accountToUpgrade;
      // scanned the private key of a watch only single address account
      final String existingAccountName =
              _mbwManager.getMetadataStorage().getLabelByAccount(accountToUpgrade.getId());
      new AlertDialog.Builder(AddAdvancedAccountActivity.this)
              .setTitle(R.string.priv_key_of_watch_only_account)
              .setMessage(getString(R.string.want_to_add_priv_key_to_watch_account, existingAccountName))
              .setNegativeButton(R.string.cancel, (dialogInterface, i) -> finishAlreadyExist(accToUpgrade.getReceiveAddress()))
              .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                 UUID accountId = accToUpgrade.getId();
                 WalletManager walletManager = _mbwManager.getWalletManager(false);
                 walletManager.deleteAccount(accToUpgrade.getId());
                 accountId = walletManager.createAccounts(new PrivateSingleConfig(key,
                         AesKeyCipher.defaultKeyCipher(), existingAccountName)).get(0);
                 finishOk(accountId, true);
              })
              .create()
              .show();
   }

   private UUID returnSAAccount(InMemoryPrivateKey key, MetadataStorage.BackupState backupState) {
      UUID acc = _mbwManager.getWalletManager(false)
              .createAccounts(new PrivateSingleConfig(key, AesKeyCipher.defaultKeyCipher())).get(0);
      _mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, backupState);
      return acc;
   }

   /**
    * SA spend account import method.
    */
   private void returnAccount(InMemoryPrivateKey key, MetadataStorage.BackupState backupState, AccountType type) {
      if (type == AccountType.SA) {
         finishOk(returnSAAccount(key, backupState), false);
      } else {
         new ImportSingleAddressAccountAsyncTask(key, backupState).execute();
      }
   }

   private void returnAccount(HdKeyNode hdKeyNode, boolean isUpgrade) {
      UUID acc = _mbwManager.getWalletManager(false)
              .createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKeyNode))).get(0);
      // set BackupState as ignored - we currently have no option to backup xPrivs after all
      _mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);
      finishOk(acc, isUpgrade);
   }

   enum AddressCheckResult {
      AccountExists, BTC, NonBtc, ETH
   }

   private class ImportReadOnlySingleAddressAccountAsyncTask extends AsyncTask<Void, Integer, AddressCheckResult> {
      private Address address;

      ImportReadOnlySingleAddressAccountAsyncTask(Address address) {
         this.address = address;
      }

      @Override
      protected AddressCheckResult doInBackground(Void... params) {
         //Check whether this address is already used in any account
         Optional<UUID> accountId = _mbwManager.getAccountId(address);
         if (accountId.isPresent()) {
            return AddressCheckResult.AccountExists;
         }

         if (address instanceof BtcAddress) {
            return AddressCheckResult.BTC;
         } else if (address instanceof EthAddress) {
            return AddressCheckResult.ETH;
         } else {
            return AddressCheckResult.NonBtc;
         }
      }

      @Override
      protected void onPostExecute(AddressCheckResult result) {
         switch (result) {
            case AccountExists:
               finishAlreadyExist(address);
               break;
            case BTC:
               UUID account1 = _mbwManager.getWalletManager(false)
                       .createAccounts(new AddressSingleConfig((BtcAddress) address)).get(0);
               finishOk(account1, false);
               break;
            case ETH:
               UUID account2 = _mbwManager.getWalletManager(false)
                       .createAccounts(new EthAddressConfig((EthAddress) address)).get(0);
               finishOk(account2, false);
               break;
            case NonBtc:
               if ("FIO".equals(address.getCoinType().getSymbol())) {
                  new FIOCreationAsyncTask((FioAddress) address).execute();
               }
         }
      }
   }

   private class FIOCreationAsyncTask extends AsyncTask<Void, Integer, UUID> {
      private ProgressDialog _progress;
      private FioAddress address;

      FIOCreationAsyncTask(FioAddress address) {
         this.address = address;
      }

      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         _progress = new ProgressDialog(AddAdvancedAccountActivity.this);
         _progress.setCancelable(false);
         _progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         _progress.setMessage(getString(R.string.fio_account_creation_started));
         _progress.show();
      }

      @Override
      protected UUID doInBackground(Void... params) {
         try {
            List<UUID> accounts = _mbwManager.getWalletManager(false).createAccounts(new FIOAddressConfig(address));
            if (accounts.size() > 0) {
               return accounts.get(0);
            }
         } catch (Exception e) {
            // ignore
         }
         return null;
      }

      @Override
      protected void onPostExecute(UUID account) {
         _progress.dismiss();
         if (account != null) {
            finishOk(account, false);
         } else {
            finishError(address, R.string.fio_account_import_error);
         }
      }
   }

   @Override
   public void finishCoCoFound(final UUID firstAddedAccount, int accountsCreated, int existingAccountsFound,
                               Value mtFound, Value massFound, Value rmcFound) {
      List<String> amountStrings = new ArrayList<>();
      for (Value found : new Value[]{rmcFound, mtFound, massFound}) {
         if (found.isPositive()) {
            amountStrings.add(ValueExtensionsKt.toStringWithUnit(found));
         }
      }
      String fundsFound = TextUtils.join(", ", amountStrings);
      String message = null;
      String accountsCreatedString = getResources().getQuantityString(R.plurals.new_accounts_created, accountsCreated,
              accountsCreated);
      String existingFoundString = getResources().getQuantityString(R.plurals.existing_accounts_found,
              existingAccountsFound, existingAccountsFound);
      if (accountsCreated > 0 && existingAccountsFound == 0) {
         message = getString(R.string.d_coco_created, fundsFound, accountsCreatedString);
      } else if (accountsCreated > 0 && existingAccountsFound > 0) {
         message = getString(R.string.d_coco_created_existing_found, fundsFound, accountsCreatedString, existingFoundString);
      } else if (existingAccountsFound > 0) {
         message = getString(R.string.d_coco_existing_found, fundsFound, existingFoundString);
      }
      new AlertDialog.Builder(this)
              .setTitle(R.string.coco_found)
              .setMessage(message)
              .setPositiveButton(R.string.button_continue, (dialogInterface, i) -> finishOk(firstAddedAccount, false))
              .create()
              .show();
   }

   @Override
   public void finishCoCoNotFound(final HdKeyNode hdKeyNode) {
      new AlertDialog.Builder(this)
              .setTitle(R.string.coco_not_found)
              .setMessage(R.string.no_digital_asset)
              .setPositiveButton(R.string.close, null)
              .setNegativeButton(R.string.rescan, (dialog, id) -> {
                 ImportCoCoHDAccount importCoCoHDAccount = new ImportCoCoHDAccount(AddAdvancedAccountActivity.this, hdKeyNode);
                 importCoCoHDAccount.setFinishListener(AddAdvancedAccountActivity.this);
                 importCoCoHDAccount.execute();
              })
              .create()
              .show();
   }

   private void finishAlreadyExist(Address address) {
      String accountType = getAccountType(address);
      Intent result = new Intent()
              .putExtra(AddAccountActivity.RESULT_MSG, getString(R.string.account_already_exist, accountType));
      setResult(RESULT_MSG, result);
      finish();
   }

   private void finishError(Address address, int res) {
      Intent result = new Intent()
              .putExtra(AddAccountActivity.RESULT_MSG, getString(res, address.toString()));
      setResult(RESULT_MSG, result);
      finish();
   }

   private String getAccountType(Address address) {
      UUID accountId = _mbwManager.getAccountId(address).get();
      WalletAccount account = _mbwManager.getWalletManager(false).getAccount(accountId);
      if (account instanceof HDAccount) {
         return "BTC HD account";
      }
      if (account instanceof SingleAddressAccount) {
         return "BTC Single Address";
      }
      return account.getCoinType().getName();
   }

   private void finishOk(UUID account, boolean isUpgrade) {
      MbwManager.getEventBus().post(new AccountCreated(account));
      MbwManager.getEventBus().post(new AccountChanged(account));
      Intent result = new Intent()
              .putExtra(AddAccountActivity.RESULT_KEY, account)
              .putExtra(AddAccountActivity.IS_UPGRADE, isUpgrade);
      setResult(RESULT_OK, result);
      finish();
   }

   enum AccountType {
      SA, Colu, Unknown
   }
}
