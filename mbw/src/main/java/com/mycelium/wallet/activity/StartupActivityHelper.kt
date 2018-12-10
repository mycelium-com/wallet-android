package com.mycelium.wallet.activity

import android.content.Context
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.LoadingProgressStatus

/**
 * This function is supposed to help convert status of wallet loading into human readable text.
 */
fun LoadingProgressStatus.format(context: Context) = when (this) {
        is LoadingProgressStatus.Starting -> context.getString(R.string.starting_application)
        is LoadingProgressStatus.Migrating -> context.getString(R.string.migrating_accounts)
        is LoadingProgressStatus.Loading -> context.getString(R.string.loading_accounts)
        is LoadingProgressStatus.MigratingNOfMHD ->
                context.getString(R.string.migrating_n_of_m_hd, formatArgumets[0], formatArgumets[1])
        is LoadingProgressStatus.LoadingNOfMHD ->
                context.getString(R.string.loading_n_of_m_hd, formatArgumets[0], formatArgumets[1])
        is LoadingProgressStatus.MigratingNOfMSA ->
                context.getString(R.string.migrating_n_of_m_sa, formatArgumets[0], formatArgumets[1])
        is LoadingProgressStatus.LoadingNOfMSA ->
                context.getString(R.string.loading_n_of_m_sa, formatArgumets[0], formatArgumets[1])
    }