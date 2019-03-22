package com.mycelium.wallet.external.changelly.bch

import com.mycelium.spvmodule.TransactionFee
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.WalletAccount
import java.math.BigDecimal


//TODO: call estimateFeeFromTransferrableAmount need refactoring, we should call account object
fun WalletAccount<*,*>.estimateFeeFromTransferrableAmount(mbwManager: MbwManager, amount: Long): BigDecimal? {
    return BigDecimal.valueOf(0)
}
