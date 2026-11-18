package com.mycelium.wallet.tron

import android.content.Context
import com.mycelium.wallet.Constants
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.tron.AbstractTronAccount

/**
 * TRON / TRC20 history view mode. By default the history shows only value transfers (and, on the
 * TRX account, the TRX fee of TRC20 sends); staking,
 * resource delegation, votes, contract calls etc. are visible after the user switches the
 * "Show all operations" menu toggle on (global, stored in app settings).
 */
object TronHistoryFilter {
    private const val PREF_SHOW_ALL = "tron_history_show_all_operations"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)

    fun isShowAll(context: Context): Boolean = prefs(context).getBoolean(PREF_SHOW_ALL, false)

    fun setShowAll(context: Context, showAll: Boolean) {
        prefs(context).edit().putBoolean(PREF_SHOW_ALL, showAll).apply()
    }

    /** History page for the UI list, honouring the TRON filter for TRON / TRC20 accounts. */
    @JvmStatic
    fun historyPage(context: Context, account: WalletAccount<*>, offset: Int, limit: Int): List<TransactionSummary> =
        if (account is AbstractTronAccount) {
            account.getTransactionSummaries(offset, limit, transfersOnly = !isShowAll(context),
                tokenContracts = MbwManager.getInstance(context).supportedTRC20Tokens.values.map { it.contractAddress })
        } else {
            account.getTransactionSummaries(offset, limit)
        }
}
