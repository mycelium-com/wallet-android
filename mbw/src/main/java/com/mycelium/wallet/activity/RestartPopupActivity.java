package com.mycelium.wallet.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.Window;

import com.mycelium.wallet.R;

public class RestartPopupActivity extends Activity {
    public static final String RESTART_WARNING_HEADER = "RESTART_WARNING_HEADER";
    public static final String RESTART_REQUIRED = "RESTART_REQUIRED";

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup_activity);
        final Boolean restartRequired = getIntent().getBooleanExtra(RESTART_REQUIRED, true);

        AlertDialog.Builder Builder = new AlertDialog.Builder(this, R.style.MyceliumModern_Dialog)
                .setTitle(Html.fromHtml(getIntent().getExtras().getString(RESTART_WARNING_HEADER, "")))
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RestartPopupActivity.this.finish();
                        if (restartRequired) restart();
                    }
                });
        if (restartRequired) Builder.setMessage(R.string.configuration_change_restart_warning);
        AlertDialog alertDialog = Builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void restart() {
        Intent intent = getBaseContext().getPackageManager()
                .getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
