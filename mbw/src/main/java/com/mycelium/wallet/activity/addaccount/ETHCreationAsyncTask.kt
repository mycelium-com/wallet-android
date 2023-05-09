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
                           val endListener: (UUID?) -> Unit) : AsyncTask<Void, Int, UUID>() {

    override fun onPreExecute() {
        super.onPreExecute()
        startListener()
    }

    override fun doInBackground(vararg params: Void?): UUID =
            mbwManager.getWalletManager(false)
                    .createAccounts(EthereumMasterseedConfig()).first()


    override fun onPostExecute(accountId: UUID?) {
        MbwManager.getEventBus().post(AccountCreated(accountId))
        MbwManager.getEventBus().post(AccountChanged(accountId))
        endListener(accountId)
    }
}

fun MbwManager.createETH(startListener: () -> Unit, endListener: (UUID?) -> Unit) {
    GlobalScope.launch(Dispatchers.Default) {
        withContext(Dispatchers.Main) {
            startListener()
        }
        val accountId = getWalletManager(false).createAccounts(EthereumMasterseedConfig()).first()
        withContext(Dispatchers.Main) {
            MbwManager.getEventBus().post(AccountCreated(accountId))
            MbwManager.getEventBus().post(AccountChanged(accountId))
            endListener(accountId)
        }
    }
}