package com.mycelium.wallet.activity.rmc;

import java.util.Calendar;
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

    public static Calendar getICOEnd() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        calendar.set(2017, 8, 29, 23, 59, 59);
        return calendar;
    }

    public static Calendar getICOStart() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        calendar.set(2017, 7, 28, 0, 0);
        return calendar;
    }
}
