package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.manager.WalletListener
import java.util.concurrent.CountDownLatch

class SynchronizeFinishedListener: WalletListener{
    var latch: CountDownLatch = CountDownLatch(1)

    override fun syncStarted() {
    }

    override fun syncStopped() {
        latch.countDown()
    }

    fun waitForSyncFinished() {
        try {
            latch.await()
        } catch (exc: InterruptedException) {}
    }

}