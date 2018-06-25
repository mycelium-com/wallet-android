package com.mycelium.wapi.api;

public interface ConnectionMonitor {
    enum ConnectionEvent {WENT_OFFLINE, WENT_ONLINE} ;
    interface ConnectionObserver {
        void connectionChanged(ConnectionEvent e);
    }
    void register(ConnectionObserver o);
    void unregister(ConnectionObserver o);
}