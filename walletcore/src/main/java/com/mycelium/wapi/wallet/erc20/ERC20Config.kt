package com.mycelium.wapi.wallet.erc20

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.manager.Config
import java.util.*

class ERC20Config constructor(val token: CryptoCurrency, val ethAccountId: UUID) : Config