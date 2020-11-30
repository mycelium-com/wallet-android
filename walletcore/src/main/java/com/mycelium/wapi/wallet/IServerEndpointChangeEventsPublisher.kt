package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.fio.ServerFioApiListChangedListener
import com.mycelium.wapi.wallet.fio.ServerFioHistoryListChangedListener

interface IServerEndpointChangeEventsPublisher {
    fun setFioServerListChangedListeners(serverFioApiListChangedListener: ServerFioApiListChangedListener,
                                         serverFioHistoryListChangedListener: ServerFioHistoryListChangedListener)
}