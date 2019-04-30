package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.manager.WalletListener
import java.util.concurrent.CountDownLatch

class SynchronizeFinishedListener(syncLock: CountDownLatch): Runnable, WalletListener{
    var latch: CountDownLatch? = null

    init {
        latch = syncLock
        Thread(this).start()
    }

    override fun syncStarted() {
        println("SynchronizeFinishedListener: Sync started")
    }

    override fun syncStopped() {
        latch?.countDown()
    }

    override fun run() {
        //todo
    }

}