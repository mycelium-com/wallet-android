package com.mycelium.wallet.external.mediaflow;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NewsSyncUtils {

    public static final int REQUEST_CODE = 1001;

    public static void startNewsUpdateRepeating(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NewsSyncReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0);

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP
                , Calendar.getInstance().getTimeInMillis() + TimeUnit.SECONDS.toMillis(30)
                , TimeUnit.MINUTES.toMillis(1), alarmIntent);
    }
}
