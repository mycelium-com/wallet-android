package com.mycelium.wallet.activity;

import android.content.Context;
import android.os.AsyncTask;

import com.mycelium.wallet.MbwManager;

import java.lang.ref.WeakReference;
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

        CreateAccountAsyncTask(Context context, AccountCreationObserver observer) {
            contextWeakReference = new WeakReference<>(context);
            observerWeakReference = new WeakReference<>(observer);
        }

        @Override
        protected UUID doInBackground(Void... params) {
            Context context = contextWeakReference.get();
            if (context == null) {
                return null;
            }
            return MbwManager.getInstance(context).createAdditionalBip44Account(context);
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
