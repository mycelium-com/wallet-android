package com.mycelium.wallet.activity.rmc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by elvis on 22.06.17.
 */

public class Keys {
    public static final String RMC_COUNT = "rmc_count";
    public static final String ETH_COUNT = "eth_count";
    public static final String PAY_METHOD = "pay_method";
    public static final String BTC_COUNT = "btc_count";
    public static final String PAYMENT_URI = "payment_uri";
    public static final String ADDRESS = "address";

    public static final int PAYMENT_REQUEST_CODE = 10002;

    public static final int TOTAL_RMC_ISSUED = 25000;
    public static final String RMC_ICO_END_DATE = "rmc_ico_end_date";

    public static Calendar getActiveStartDay() {
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.set(2017, 6, 12);
        return calendarStart;
    }

    public static Calendar getActiveEndDay() {
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.set(2018, 5, 10);
        return calendarEnd;
    }

    public static Calendar getICOEnd(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences("rmc_stats", Context.MODE_PRIVATE);
        String enddate = preferences.getString(RMC_ICO_END_DATE, null);
        Calendar calendar = null;
        calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        try {
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(enddate));
        } catch (Exception e) {
            calendar.set(2017, 8, 29, 23, 59, 59);
        }
        AsyncTask asyncTask = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] objects) {
                String icoEnd = BtcPoolStatisticsManager.getICOEnd();
                if(icoEnd != null) {
                    preferences.edit().putString(RMC_ICO_END_DATE, icoEnd).apply();
                }
                return null;
            }
        };
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        return calendar;
    }

    public static Calendar getICOStart() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        calendar.set(2017, 7, 28, 0, 0);
        return calendar;
    }
}
