package com.mycelium.wallet;

import com.mycelium.generated.wallet.database.LogsQueries;
import com.mycelium.generated.wallet.database.WalletDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CommonLogHandler extends Handler {


    private final LogsQueries logsQueries;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

    public CommonLogHandler(WalletDB db) {
        logsQueries = db.getLogsQueries();
    }

    @Override
    public void publish(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(record.getMillis())))
                .append(" - ")
                .append(record.getSourceClassName())
                .append("#")
                .append(record.getSourceMethodName())
                .append(" - ")
                .append(record.getMessage());
        System.out.println(sb.toString());
        logsQueries.insert(record.getMillis(),record.getLevel().getName(),record.getMessage());
    }

    public void cleanUp() {
        logsQueries.delete();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
