package com.mycelium.wapi.wallet;

public interface CommonAccountBacking {
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();
    void clear();
}
