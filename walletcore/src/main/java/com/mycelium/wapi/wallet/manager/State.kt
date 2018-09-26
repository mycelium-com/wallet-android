package com.mycelium.wapi.wallet.manager


enum class State {

    /**
     * The wallet not initialized
     */
    OFF,
    /**
     * The wallet manager is synchronizing
     */
    SYNCHRONIZING,

    /*
     * A fast sync (only a limited subset of all addresses) is running
     */
    FAST_SYNC,

    /**
     * The wallet manager is ready
     */
    READY
}