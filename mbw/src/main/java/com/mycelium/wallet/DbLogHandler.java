package com.mycelium.wallet;

import com.mycelium.generated.wallet.database.LogsQueries;
import com.mycelium.generated.wallet.database.WalletDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DbLogHandler extends Handler {

    private final LogsQueries logsQueries;

    public DbLogHandler(WalletDB db) {
        logsQueries = db.getLogsQueries();
    }

    @Override
    public void publish(LogRecord record) {
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
