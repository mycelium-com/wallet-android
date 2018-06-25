package com.mycelium.wallet.modularisation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;

import com.mycelium.modularizationtools.model.Module;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.R;
import com.mycelium.wallet.WalletApplication;

import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ModularisationVersionHelper {
    private static final String MODULE_PREFS = "modules_prefs";
    private static final String UPDATE_REQUIRED = "update_required";

    /**
     * Shows one AlertDialog per module that runs under a different otherModuleApiVersion, advising to upgrade the lower between wallet and module.
     */
    public static void notifyWrongModuleVersion(final Activity parent) {
        final SharedPreferences sharedPreferences = parent.getSharedPreferences(MODULE_PREFS, MODE_PRIVATE);
        sharedPreferences.getBoolean(UPDATE_REQUIRED, false);
        List<WalletApplication.ModuleVersionError> errorList = WalletApplication.getInstance().moduleVersionErrors;
        for (WalletApplication.ModuleVersionError moduleVersionError : errorList) {
            Module module = GooglePlayModuleCollection.getModuleByPackage(parent, moduleVersionError.moduleId);
            Module wallet = new Module(BuildConfig.APPLICATION_ID, "<b>Mycelium Wallet</b>", "<b>Mycelium Wallet</b>", "", "");
            final Module needsUpdate;
            if (moduleVersionError.expected > com.mycelium.modularizationtools.BuildConfig.ModuleApiVersion) {
                needsUpdate = wallet;
            } else {
                needsUpdate = module;
            }
            if (isDialogRequired(sharedPreferences, needsUpdate)) {
                new AlertDialog.Builder(parent)
                        .setTitle(Html.fromHtml(parent.getString(R.string.update_required_for, needsUpdate.getName())))
                        .setMessage(Html.fromHtml(parent.getString(R.string.update_required_details, wallet.getName(), module.getName(), needsUpdate.getName())))
                        .setPositiveButton(Html.fromHtml(parent.getString(R.string.update_for, needsUpdate.getName())), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent installIntent = new Intent(Intent.ACTION_VIEW)
                                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=" + needsUpdate.getModulePackage()));
                                parent.startActivity(installIntent);
                            }
                        })
                        .setNegativeButton(Html.fromHtml(parent.getString(R.string.continue_without, module.getShortName())), null)
                        .create()
                        .show();
                sharedPreferences.edit()
                        .putBoolean(UPDATE_REQUIRED + needsUpdate.getModulePackage(), true)
                        .apply();
            }
        }
        if (errorList.isEmpty()) {
            sharedPreferences.edit()
                    .clear()
                    .apply();
        }
    }

    private static boolean isDialogRequired(SharedPreferences sharedPreferences, Module module) {
        return !sharedPreferences.getBoolean(UPDATE_REQUIRED + module.getModulePackage(), false);
    }

    public static boolean isUpdateRequired(Context context, String modulePackage) {
        return context.getSharedPreferences(MODULE_PREFS, MODE_PRIVATE)
                .getBoolean(UPDATE_REQUIRED + modulePackage, false);
    }
}
