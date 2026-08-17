package com.mycelium.wapi.wallet.trc20

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.tron.TronAccount

/** Creates a TRC20 token account on the address of [tronAccount] (which pays the TRX fees). */
class TRC20Config(val token: CryptoCurrency, val tronAccount: TronAccount) : Config
