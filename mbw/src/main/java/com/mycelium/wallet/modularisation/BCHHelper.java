package com.mycelium.wallet.modularisation;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.mycelium.modularizationtools.CommunicationManager;
import com.mycelium.modularizationtools.model.Module;
import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.SpvBalanceFetcher;
import com.mycelium.wapi.wallet.WalletAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class BCHHelper {

    private static final String BCH_FIRST_UPDATE = "bch_first_update_page";
    private static final String BCH_FIRST_INSTALLED = "bch_first_installed_page";
    public static final String BCH_PREFS = "bch_prefs";
    public static final String IS_FIRST_SYNC = "is_first_sync";
    public static final String ALREADY_FOUND_ACCOUNT = "already_found_account";

    public static void firstBCHPages(final Context context) {
        final Module bchModule = GooglePlayModuleCollection.getModules(context).get("bch");
        final SharedPreferences sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE);
        boolean moduleBCHInstalled = CommunicationManager.getInstance(context).getPairedModules().contains(bchModule);
        if (!sharedPreferences.getBoolean(BCH_FIRST_UPDATE, false) && !moduleBCHInstalled) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.first_modulization_title)
                    .setMessage(R.string.first_modulization_message)
                    .setPositiveButton(R.string.install_bch_module, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent installIntent = new Intent(Intent.ACTION_VIEW);
                            installIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id="
                                    + bchModule.getModulePackage()
                            ));
                            context.startActivity(installIntent);
                        }
                    })
                    .setNegativeButton(R.string.contunue_without_bch, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            sharedPreferences.edit().putBoolean(BCH_FIRST_UPDATE, true)
                                    .apply();
                        }
                    })
                    .create().show();
        } else if (!sharedPreferences.getBoolean(BCH_FIRST_INSTALLED, false) && moduleBCHInstalled) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.first_bch_installed_title)
                    .setMessage(R.string.first_bch_installed_message)
                    .setPositiveButton(R.string.button_continue, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            sharedPreferences.edit().putBoolean(BCH_FIRST_INSTALLED, true)
                                    .apply();
                        }
                    })
                    .create().show();
        }
    }

    public static void bchSynced(Context context) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(BCH_PREFS, MODE_PRIVATE);
        List<WalletAccount> accounts = new ArrayList<>();
        accounts.addAll(AccountManager.INSTANCE.getBCHSingleAddressAccounts().values());
        accounts.addAll(AccountManager.INSTANCE.getBCHBip44Accounts().values());
        BigDecimal sum = BigDecimal.ZERO;
        int accountFounded = 0;
        for (WalletAccount account : accounts) {
            if (!sharedPreferences.getBoolean(ALREADY_FOUND_ACCOUNT + account.getId().toString(), false)) {
                sum = sum.add(account.getCurrencyBasedBalance().confirmed.getValue());
                accountFounded++;
                sharedPreferences.edit()
                        .putBoolean(ALREADY_FOUND_ACCOUNT + account.getId().toString(), true)
                        .apply();
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.scaning_complete);
        builder.setPositiveButton(R.string.button_continue, null);
        if (sum.floatValue() > 0) {
            builder.setMessage(context.getString(R.string.bch_accounts_found,
                    sum.toPlainString()
                    , accountFounded));
            builder.create().show();
        } else if (sharedPreferences.getBoolean(IS_FIRST_SYNC, true)) {
            sharedPreferences.edit().putBoolean(IS_FIRST_SYNC, false).apply();
            builder.setMessage(R.string.bch_accounts_not_found);
            builder.create().show();
        }
    }

    public static int getBCHSyncProgress(Context context) {
        SpvBalanceFetcher spvBalanceFetcher = MbwManager.getInstance(context).getSpvBchFetcher();
        int result = 0;
        if (spvBalanceFetcher != null) {
            result = spvBalanceFetcher.getSyncProgressPercents();
        }
        return result;
    }

    public static void bchTechnologyPreviewDialog(Context context) {
        new AlertDialog.Builder(context)
                .setMessage(R.string.bch_technology_preview)
                .setPositiveButton(R.string.button_ok, null).create().show();
    }
}
