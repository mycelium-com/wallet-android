package com.mycelium.wallet.activity.main.loader

import android.os.AsyncTask
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import java.util.concurrent.atomic.AtomicBoolean


class Preloader(private val toAdd: MutableList<TransactionSummary>,
                private val account: WalletAccount<*>,
                private val offset: Int, private val limit: Int,
                private val success: AtomicBoolean)
    : AsyncTask<Void?, Void?, Void?>() {
    override fun doInBackground(vararg params: Void?): Void? {
        val preloadedData = account.getTransactionSummaries(offset, limit)

        synchronized(toAdd) {
            toAdd.addAll(preloadedData)
            success.set(toAdd.size == limit)
        }
        return null
    }
}
