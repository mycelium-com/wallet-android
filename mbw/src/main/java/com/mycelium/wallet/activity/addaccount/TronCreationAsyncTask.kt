package com.mycelium.wallet.activity.addaccount

import android.os.AsyncTask
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountCreated
import com.mycelium.wapi.wallet.tron.TronMasterseedConfig
import java.util.UUID

class TronCreationAsyncTask(val mbwManager: MbwManager,
                            val startListener: () -> Unit,
                            val endListener: (UUID?) -> Unit) : AsyncTask<Void, Int, UUID>() {

    override fun onPreExecute() {
        super.onPreExecute()
        startListener()
    }

    override fun doInBackground(vararg params: Void?): UUID =
        mbwManager.getWalletManager(false).createAccounts(TronMasterseedConfig()).first()

    override fun onPostExecute(accountId: UUID?) {
        MbwManager.getEventBus().post(AccountCreated(accountId))
        MbwManager.getEventBus().post(AccountChanged(accountId))
        endListener(accountId)
    }
}
