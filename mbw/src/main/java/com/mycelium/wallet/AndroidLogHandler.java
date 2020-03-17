package com.mycelium.wallet;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

class AndroidLogHandler extends Handler {
    private static String ANDROID_LOG_TAG = "AndroidLogHandler";
    final private static int SEVERE_LOG = 1000;
    final private static int WARNING_LOG = 900;
    final private static int INFO_LOG = 800;
    public AndroidLogHandler() {
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void publish(LogRecord record) {
        Date date = new Date(record.getMillis());
        String dateAsString = dateFormat.format(date);
        String message = dateAsString + ":" + record.getLevel().getName() + ": " + record.getMessage();
        switch (record.getLevel().intValue()) {
            case SEVERE_LOG: {
                Log.e(ANDROID_LOG_TAG, message);
            }
            break;
            case WARNING_LOG: {
                Log.w(ANDROID_LOG_TAG, message);
            }
            break;
            case INFO_LOG: {
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
