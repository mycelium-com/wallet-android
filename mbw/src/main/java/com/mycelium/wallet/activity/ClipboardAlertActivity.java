package com.mycelium.wallet.activity;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.service.ClipboardMonitorService;

public class ClipboardAlertActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clipboard_alert);
        final Activity activity = this;
        findViewById(R.id.buttonStopWatchdog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardMonitorService.stoppedExplicitly = true;
                activity.stopService(new Intent(activity, ClipboardMonitorService.class));
                activity.finish();
            }
        });
        findViewById(R.id.buttonClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.finish();
            }
        });
    }
}
