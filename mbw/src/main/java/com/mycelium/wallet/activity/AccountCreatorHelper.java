package com.mycelium.wallet.activity;

import android.content.Context;
import android.os.AsyncTask;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44Account;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * This is used to create an account if it failed to be created in ConfigureSeedAsyncTask.
 * Resolves crashes that some users experience
 */
final class AccountCreatorHelper {
    public interface AccountCreatable {
        MbwManager getMbwManager();
        void finishActivity(UUID accountId);
    }

    public static class CreateAccountAsyncTask extends AsyncTask<Void, Integer, UUID> {
        private WeakReference<AccountCreatable> weakActivity;

        CreateAccountAsyncTask(AccountCreatable activity) {
            weakActivity = new WeakReference<>(activity);
        }

        @Override
        protected UUID doInBackground(Void... params) {
            AccountCreatable activity = weakActivity.get();
            if (activity == null) {
                return null;
            }
            try {
                WalletManager walletManager = activity.getMbwManager().getWalletManager(false);
                return walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
            } catch (KeyCipher.InvalidKeyCipher e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(UUID accountId) {
            AccountCreatable activity = weakActivity.get();
            if (accountId == null || activity == null) {
                return;
            }
            //set default label for the created HD account
            WalletAccount account = activity.getMbwManager().getWalletManager(false).getAccount(accountId);
            String defaultName = Utils.getNameForNewAccount(account, (Context) activity);
            activity.getMbwManager().getMetadataStorage().storeAccountLabel(accountId, defaultName);
            //finish initialization
            activity.finishActivity(accountId);
        }
    }
}
