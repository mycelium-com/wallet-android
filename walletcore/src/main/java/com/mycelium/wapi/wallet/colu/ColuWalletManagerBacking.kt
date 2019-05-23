package com.mycelium.wapi.wallet.colu

import com.mycelium.wapi.wallet.SecureKeyValueStoreBacking
import com.mycelium.wapi.wallet.SingleAddressBtcAccountBacking
import com.mycelium.wapi.wallet.WalletBacking
import com.mycelium.wapi.wallet.btc.Bip44BtcAccountBacking
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import java.util.*

interface ColuWalletManagerBacking<AccountContext>: WalletBacking<AccountContext>, SecureKeyValueStoreBacking {

}