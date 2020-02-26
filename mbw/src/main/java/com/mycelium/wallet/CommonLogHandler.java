package com.mycelium.wallet;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CommonLogHandler extends Handler {

  @Override
  public void publish(LogRecord record) {
      StringBuilder sb = new StringBuilder();
      sb.append(record.getMillis())
        .append(" - ")
        .append(record.getSourceClassName())
        .append("#")
        .append(record.getSourceMethodName())
        .append(" - ")
        .append(record.getMessage());
      System.out.println(sb.toString());
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() throws SecurityException {
  }
}
