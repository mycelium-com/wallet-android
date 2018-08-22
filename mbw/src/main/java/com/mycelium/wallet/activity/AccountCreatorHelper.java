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
    public interface AccountCreatable {
        void finishActivity(UUID accountId);
    }

    public static class CreateAccountAsyncTask extends AsyncTask<Void, Integer, UUID> {
        private WeakReference<Context> contextWeakReference;
        private WeakReference<AccountCreatable> accountCreatableWeakReference;

        CreateAccountAsyncTask(Context context, AccountCreatable accountCreatable) {
            contextWeakReference = new WeakReference<>(context);
            accountCreatableWeakReference = new WeakReference<>(accountCreatable);
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
            AccountCreatable accountCreatable = accountCreatableWeakReference.get();
            if (accountCreatable != null) {
                accountCreatable.finishActivity(uuid);
            }
        }
    }
}
