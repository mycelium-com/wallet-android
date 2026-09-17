package com.mycelium.wallet.activity.addaccount

import android.os.AsyncTask
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountCreated
import com.mycelium.wapi.wallet.eth.EthereumMasterseedConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class ETHCreationAsyncTask(val mbwManager: MbwManager,
                           val startListener: () -> Unit,
                           val endListener: (UUID?) -> Unit) : AsyncTask<Void, Int, UUID?>() {

    override fun onPreExecute() {
        super.onPreExecute()
        startListener()
    }

    // empty when the module refuses (an unused ETH account already exists)
    override fun doInBackground(vararg params: Void?): UUID? =
            mbwManager.getWalletManager(false)
                    .createAccounts(EthereumMasterseedConfig()).firstOrNull()


    override fun onPostExecute(accountId: UUID?) {
        if (accountId != null) {
            MbwManager.getEventBus().post(AccountCreated(accountId))
            MbwManager.getEventBus().post(AccountChanged(accountId))
        }
        endListener(accountId)
    }
}

fun MbwManager.createETH(startListener: () -> Unit, endListener: (UUID?) -> Unit) {
    GlobalScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            startListener()
        }
        val accountId = getWalletManager(false).createAccounts(EthereumMasterseedConfig()).firstOrNull()
        withContext(Dispatchers.Main) {
            if (accountId != null) {
                MbwManager.getEventBus().post(AccountCreated(accountId))
                MbwManager.getEventBus().post(AccountChanged(accountId))
            }
            endListener(accountId)
        }
    }
}