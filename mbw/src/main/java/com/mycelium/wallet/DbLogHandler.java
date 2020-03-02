package com.mycelium.wallet;

import com.mycelium.generated.wallet.database.LogsQueries;
import com.mycelium.generated.wallet.database.WalletDB;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DbLogHandler extends Handler {

    private static final Integer MAX_LOG_RECORDS = 50000;
    private final LogsQueries logsQueries;

    public DbLogHandler(WalletDB db) {
        logsQueries = db.getLogsQueries();
    }

    @Override
    public void publish(LogRecord record) {
        logsQueries.insert(record.getMillis(), record.getLevel().getName(), record.getMessage());
    }

    public void cleanUp() {
        logsQueries.cleanUp(MAX_LOG_RECORDS);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
