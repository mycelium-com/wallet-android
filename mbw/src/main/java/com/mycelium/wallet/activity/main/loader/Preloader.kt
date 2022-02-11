package com.mycelium.wallet.activity.main.loader

import android.os.AsyncTask
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIOOBTransaction
import com.mycelium.wapi.wallet.fio.FioModule
import java.util.concurrent.atomic.AtomicBoolean


class Preloader(private val toAdd: MutableList<TransactionSummary>,
                private var fioMetadataMap: MutableMap<String, FIOOBTransaction>,
                private val account: WalletAccount<*>,
                private val mbwManager: MbwManager,
                private val offset: Int, private val limit: Int,
                private val success: AtomicBoolean)
    : AsyncTask<Void?, Void?, Void?>() {
    override fun doInBackground(vararg params: Void?): Void? {
        val preloadedData = account.getTransactionSummaries(offset, limit)
        for (txSummary in preloadedData) {
            (mbwManager.getWalletManager(false).getModuleById(FioModule.ID) as FioModule?)
                    ?.getFioTxMetadata(txSummary.idHex)?.let { data ->
                        fioMetadataMap[txSummary.idHex] = data
                    }
        }

        synchronized(toAdd) {
            toAdd.addAll(preloadedData)
            success.set(toAdd.size == limit)
        }
        return null
    }
}
