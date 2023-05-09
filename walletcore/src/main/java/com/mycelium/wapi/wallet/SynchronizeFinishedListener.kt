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

    override fun accountSyncStopped(walletAccount: WalletAccount<*>) {
    }

    fun waitForSyncFinished() {
        try {
            latch.await()
        } catch (exc: InterruptedException) {}
    }

}