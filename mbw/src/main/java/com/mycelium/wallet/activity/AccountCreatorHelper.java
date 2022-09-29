package com.mycelium.wallet.activity;

import android.content.Context;
import android.os.AsyncTask;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wapi.wallet.manager.Config;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

/**
 * This is used to create an account if it failed to be created in ConfigureSeedAsyncTask.
 * Resolves crashes that some users experience
 */
final class AccountCreatorHelper {
    public interface AccountCreationObserver {
        void onAccountCreated(UUID accountId);
    }

    public static class CreateAccountAsyncTask extends AsyncTask<Void, Integer, UUID> {
        private WeakReference<Context> contextWeakReference;
        private WeakReference<AccountCreationObserver> observerWeakReference;
        private List<Config> accounts;

        CreateAccountAsyncTask(Context context, AccountCreationObserver observer, List<Config> accounts) {
            contextWeakReference = new WeakReference<>(context);
            observerWeakReference = new WeakReference<>(observer);
            this.accounts = accounts;
        }

        @Override
        protected UUID doInBackground(Void... params) {
            Context context = contextWeakReference.get();
            if (context == null) {
                return null;
            }
            final List<UUID> uuids = MbwManager.getInstance(context)
                    .createAdditionalBip44AccountsUninterruptedly(accounts);
            if (uuids.isEmpty())
                return null;
            return uuids.get(0);
        }

        @Override
        protected void onPostExecute(UUID uuid) {
            AccountCreationObserver observer = observerWeakReference.get();
            if (observer != null) {
                observer.onAccountCreated(uuid);
            }
        }
    }
}
