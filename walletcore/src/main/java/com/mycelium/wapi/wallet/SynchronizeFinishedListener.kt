package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.manager.WalletListener
import java.util.concurrent.CountDownLatch

class SynchronizeFinishedListener: Runnable, WalletListener{
    var latch: CountDownLatch? = null

    init {
        latch = CountDownLatch(1)
    }

    override fun syncStarted() {
        println("SynchronizeFinishedListener: Sync started")
        Thread(this).start()
    }

    override fun syncStopped() {
        latch?.countDown()
    }

    override fun run() {
        try {
            latch?.await()
        } catch (exc: InterruptedException) {
            System.out.println("WalletConsole: Sync account exception");
        }
    }

}