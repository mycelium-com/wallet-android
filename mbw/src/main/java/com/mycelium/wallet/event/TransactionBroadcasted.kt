package com.mycelium.wallet.event

import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType

class TransactionBroadcasted @JvmOverloads constructor(val txid: String?,
                                                           val result: BroadcastResult = BroadcastResult(BroadcastResultType.SUCCESS))