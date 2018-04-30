package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
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

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.popup_activity);
        AlertDialog.Builder Builder = new AlertDialog.Builder(this, R.style.MyceliumModern_Dialog)
                .setTitle(Html.fromHtml(getIntent().getExtras().getString(RESTART_WARNING_HEADER, "")))
                .setMessage(R.string.configuration_change_restart_warning)
                .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RestartPopupActivity.this.finish();
                        restart(getApplicationContext());
                    }
                });
        AlertDialog alertDialog = Builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private void restart(Context context) {
        Intent futureActivity = new Intent(context, StartupActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                futureActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
        Runtime.getRuntime().exit(0);
    }
}
