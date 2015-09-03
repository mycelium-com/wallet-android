package com.mycelium.wallet.ledger.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mycelium.wallet.*;
import com.mycelium.wallet.activity.util.Pin;
import com.mycelium.wallet.ledger.LedgerManager;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.squareup.otto.Subscribe;
import nordpol.android.TagDispatcher;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;


public class LedgerAccountImportActivity extends LedgerAccountSelectorActivity {

   private final LedgerManager ledgerManager = MbwManager.getInstance(this).getLedgerManager();
   private TagDispatcher dispatcher;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      dispatcher = TagDispatcher.get(this, ledgerManager);
   }

   @Override
   protected void onResume() {
      super.onResume();
      updateUi();
      dispatcher.enableExclusiveNfc();
   }

   @Override
   protected void onPause() {
      super.onPause();
      dispatcher.disableExclusiveNfc();
   }

   @Override
   protected void onNewIntent(Intent intent) {
      dispatcher.interceptIntent(intent);
   }

   public static void callMe(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, LedgerAccountImportActivity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   @Override
   protected AdapterView.OnItemClickListener accountClickListener() {
      return new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            HdAccountWrapper item = (HdAccountWrapper) adapterView.getItemAtPosition(i);

            // create the new account and get the uuid of it
            MbwManager mbwManager = MbwManager.getInstance(LedgerAccountImportActivity.this);

            UUID acc = mbwManager.getWalletManager(false)
                  .createExternalSignatureAccount(item.xPub, (LedgerManager) masterseedScanManager, item.accountIndex);

            // Mark this account as backup warning ignored
            mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);

            Intent result = new Intent();
            result.putExtra("account", acc);
            setResult(RESULT_OK, result);
            finish();
         }
      };
   }

   @Override
   protected void updateUi() {
      super.updateUi();
      if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.done) {
         findViewById(R.id.btNextAccount).setEnabled(true);
      } else {
         findViewById(R.id.btNextAccount).setEnabled(false);
      }
   }

   @Override
   protected void setView() {
      setContentView(R.layout.activity_instant_ledger);
      ((TextView) findViewById(R.id.tvCaption)).setText(getString(R.string.ledger_import_account_caption));
      ((TextView) findViewById(R.id.tvSelectAccount)).setText(getString(R.string.ledger_select_account_to_import));
      ((TextView) findViewById(R.id.btNextAccount)).setVisibility(View.VISIBLE);

      findViewById(R.id.btNextAccount).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            Utils.showSimpleMessageDialog(LedgerAccountImportActivity.this, getString(R.string.ledger_next_unused_account_info), new Runnable() {
               @Override
               public void run() {

                  // The TEE calls are asynchronous, and simulate a blocking call
                  // To avoid locking up the UI thread, a new one is started

                  new Thread() {
                     public void run() {
                        Optional<HdKeyNode> nextAccount = masterseedScanManager.getAccountPubKeyNode(accounts.size());

                        MbwManager mbwManager = MbwManager.getInstance(LedgerAccountImportActivity.this);

                        if (nextAccount.isPresent()) {
                           final UUID acc = mbwManager.getWalletManager(false)
                                 .createExternalSignatureAccount(nextAccount.get(), (LedgerManager) masterseedScanManager, accounts.size());

                           mbwManager.getMetadataStorage().setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED);

                           runOnUiThread(new Runnable() {
                              public void run() {
                                 Intent result = new Intent();
                                 result.putExtra("account", acc);
                                 setResult(RESULT_OK, result);
                                 finish();
                              }
                           });
                        }

                     }
                  }.start();

               }
            });
         }
      });
   }

   @Subscribe()
   public void onPinRequest(LedgerManager.OnPinRequest event) {
      LedgerPinDialog pin = new LedgerPinDialog(this, true);
      pin.setTitle(R.string.ledger_enter_pin);
      pin.setOnPinValid(new PinDialog.OnPinEntered() {
         @Override
         public void pinEntered(PinDialog dialog, Pin pin) {
            ((LedgerManager) masterseedScanManager).enterPin(pin.getPin());
            dialog.dismiss();
         }
      });
      pin.show();
   }

   // Otto.EventBus does not traverse class hierarchy to find subscribers
   @Subscribe
   public void onScanError(AccountScanManager.OnScanError event){
      super.onScanError(event);
   }

   @Subscribe
   public void onStatusChanged(AccountScanManager.OnStatusChanged event){
      super.onStatusChanged(event);
   }

   @Subscribe
   public void onAccountFound(AccountScanManager.OnAccountFound event){
      super.onAccountFound(event);
   }

   @Subscribe
   public void onPassphraseRequest(AccountScanManager.OnPassphraseRequest event){
      super.onPassphraseRequest(event);
   }

}
