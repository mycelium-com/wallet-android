package com.mycelium.wapi.wallet.erc20

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.manager.Config

class ERC20Config constructor(val token: CryptoCurrency, val ethAccount: EthAccount) : Config