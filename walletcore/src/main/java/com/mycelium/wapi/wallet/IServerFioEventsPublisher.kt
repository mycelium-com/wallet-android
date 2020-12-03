package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.fio.FioTpidChangedListener
import com.mycelium.wapi.wallet.fio.ServerFioApiListChangedListener
import com.mycelium.wapi.wallet.fio.ServerFioHistoryListChangedListener

interface IServerFioEventsPublisher {
    fun setFioServerListChangedListeners(serverFioApiListChangedListener: ServerFioApiListChangedListener,
                                         serverFioHistoryListChangedListener: ServerFioHistoryListChangedListener)

    fun setFioTpidChangedListener(fioTpidChangedListener: FioTpidChangedListener)
}