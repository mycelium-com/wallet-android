package com.mycelium.wallet.activity.addaccount

import android.os.AsyncTask
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountCreated
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.trc20.TRC20Config
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.TronAccount
import java.util.UUID

class TRC20CreationAsyncTask(val mbwManager: MbwManager,
                             val tokens: List<TRC20Token>,
                             val tronAccount: TronAccount,
                             val startListener: () -> Unit,
                             val endListener: (List<UUID>) -> Unit) : AsyncTask<Void, Int, List<UUID>>() {

    override fun onPreExecute() {
        super.onPreExecute()
        if (tokens.isEmpty()) {
            cancel(false)
        } else {
            startListener()
        }
    }

    override fun doInBackground(vararg params: Void?): List<UUID> =
        tokens.flatMap { token ->
            mbwManager.getWalletManager(false)
                .createAccounts(TRC20Config(token, tronAccount))
                .apply { tronAccount.updateEnabledTokens() }
        }

    override fun onPostExecute(accountIds: List<UUID>) {
        accountIds.forEach { accountId ->
            mbwManager.metadataStorage.setOtherAccountBackupState(accountId, MetadataStorage.BackupState.IGNORED)
            MbwManager.getEventBus().post(AccountCreated(accountId))
            MbwManager.getEventBus().post(AccountChanged(accountId))
        }
        endListener(accountIds)
    }
}
