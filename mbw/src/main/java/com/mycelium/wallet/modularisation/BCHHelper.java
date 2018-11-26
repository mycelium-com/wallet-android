package com.mycelium.wallet.modularisation;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.model.Module;
import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.WalletAccount;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class BCHHelper {
    public static final String BCH_FIRST_UPDATE = "bch_first_update_page";
    private static final String BCH_INSTALLED = "bch_installed_page";
    public static final String BCH_PREFS = "bch_prefs";
    public static final String ALREADY_FOUND_ACCOUNT = "already_found_account";
    public static final String IS_ACCOUNT_VISIBLE = "is_account_visible";
    private static final String IS_FIRST_SYNC = "is_first_sync";

    public static void firstBCHPages(final Context context) {
        final Module bchModule = GooglePlayModuleCollection.getModules(context).get("bch");
        final SharedPreferences sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE);
        final boolean moduleBCHInstalled = Utils.isAppInstalled(context, bchModule.getModulePackage());
        if (!sharedPreferences.getBoolean(BCH_INSTALLED, false) && moduleBCHInstalled) {
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_bch_module_installed, null);
            ((TextView) view.findViewById(R.id.title)).setText(Html.fromHtml(context.getString(R.string.first_bch_installed_title)));
            ((TextView) view.findViewById(R.id.content)).setText(Html.fromHtml(context.getString(R.string.to_get_your_bitcoin_cash_retrieved)));
            final AlertDialog dialog = new AlertDialog.Builder(context, R.style.MyceliumModern_Dialog)
                    .setView(view)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            sharedPreferences.edit()
                                    .putBoolean(BCH_FIRST_UPDATE, true)
                                    .putBoolean(BCH_INSTALLED, true)
                                    .apply();
                        }
                    })
                    .create();

            view.findViewById(R.id.buttonContinue).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
        if (sharedPreferences.getBoolean(BCH_INSTALLED, false) && !moduleBCHInstalled) {
            sharedPreferences.edit().putBoolean(BCH_INSTALLED, false)
                    .apply();
        }
    }

    public static void bchSynced(Context context) {
        class BCHSyncedAsyncTask extends AsyncTask<Void, Void, Void> {
            private final WeakReference<Context> contextRefence;
            private volatile BigDecimal sum;
            private volatile int accountsFound;
            private SharedPreferences sharedPreferences;

            public BCHSyncedAsyncTask(Context context) {
                this.contextRefence = new WeakReference<>(context);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Context context = contextRefence.get();
                if (context == null) {
                    cancel(true);
                    return null;
                }
                sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE);
                List<WalletAccount> accounts = new ArrayList<>();
                accounts.addAll(AccountManager.INSTANCE.getBCHSingleAddressAccounts().values());
                accounts.addAll(AccountManager.INSTANCE.getBCHBip44Accounts().values());
                sum = BigDecimal.ZERO;
                accountsFound = 0;
                for (WalletAccount account : accounts) {
                    if (!sharedPreferences.getBoolean(ALREADY_FOUND_ACCOUNT + account.getId().toString(), false)) {
                        sum = sum.add(account.getCurrencyBasedBalance().confirmed.getValue());
                        accountsFound++;
                        sharedPreferences.edit()
                                .putBoolean(ALREADY_FOUND_ACCOUNT + account.getId().toString(), true)
                                .apply();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Context context = contextRefence.get();
                if (context == null) {
                    cancel(true);
                }
                View view = LayoutInflater.from(context).inflate(R.layout.dialog_bch_found, null);
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.MyceliumModern_Dialog)
                        .setView(view);
                final AlertDialog dialog = builder.create();
                view.findViewById(R.id.buttonContinue).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
                if (sum.floatValue() > 0) {
                    ((TextView) view.findViewById(R.id.title)).setText(Html.fromHtml(context.getString(R.string.scaning_complete_found)));
                    ((TextView) view.findViewById(R.id.content)).setText(Html.fromHtml(context.getString(R.string.bch_accounts_found,
                            sum.toPlainString()
                            , accountsFound)));
                    dialog.show();
                } else if (sharedPreferences.getBoolean(IS_FIRST_SYNC, true)) {
                    ((TextView) view.findViewById(R.id.title)).setText(Html.fromHtml(context.getString(R.string.scaning_complete_not_found)));
                    ((TextView) view.findViewById(R.id.content)).setText(Html.fromHtml(context.getString(R.string.bch_accounts_not_found)));
                    dialog.show();
                }
                sharedPreferences.edit().putBoolean(IS_FIRST_SYNC, false).apply();
            }
        }

        new BCHSyncedAsyncTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static boolean isModulePaired(Context context) {
        final Module bchModule = GooglePlayModuleCollection.getModules(context).get("bch");
        return CommunicationManager.getInstance().getPairedModules().contains(bchModule);
    }

    public static float getBCHSyncProgress(Context context) {
        SpvBalanceFetcher spvBalanceFetcher = MbwManager.getInstance(context).getSpvBchFetcher();
        float result = 0f;
        if (spvBalanceFetcher != null) {
            result = spvBalanceFetcher.getSyncProgressPercents();
        }
        return result;
    }

    public static void bchTechnologyPreviewDialog(Context context) {
        new AlertDialog.Builder(context, R.style.MyceliumModern_Dialog)
                .setMessage(Html.fromHtml(context.getString(R.string.bch_technology_preview)))
                .setPositiveButton(R.string.button_ok, null).create().show();
    }
}
