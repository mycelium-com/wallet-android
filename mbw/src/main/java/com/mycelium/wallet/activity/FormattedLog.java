package com.mycelium.wallet.activity;

import android.annotation.SuppressLint;

import com.mycelium.generated.logger.database.Logs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormattedLog {
        @SuppressLint("SimpleDateFormat")
        private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        private Logs log;

        public FormattedLog(Logs log) {
            this.log = log;
        }

        @Override
        public String toString() {
            return dateFormat.format(new Date(log.getDateMillis())) + ":" + log.getLevel() + ":" + log.getMessage();
        }
    }