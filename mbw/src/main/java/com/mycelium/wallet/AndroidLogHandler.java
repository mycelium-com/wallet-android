package com.mycelium.wallet;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class AndroidLogHandler extends Handler {
    private static String ANDROID_LOG_TAG = "AndroidLogHandler";

    public AndroidLogHandler() {
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void publish(LogRecord record) {
        Date date = new Date(record.getMillis());
        String dateAsString = dateFormat.format(date);
        String message = dateAsString + ":" + record.getLevel().getName() + ": " + record.getMessage();
        switch (record.getLevel().intValue()) {
            case 1000: {
                Log.e(ANDROID_LOG_TAG, message);
            }
            break;
            case 900: {
                Log.w(ANDROID_LOG_TAG, message);
            }
            break;
            case 800: {
                Log.d(ANDROID_LOG_TAG, message);
            }
            break;
            default: {
                Log.wtf(ANDROID_LOG_TAG, message);
            }

        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
