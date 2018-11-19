package com.mycelium.wallet.external.changelly.bch

import com.mycelium.spvmodule.TransactionFee
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_FROM_MASTERSEED
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue
import java.math.BigDecimal


//TODO: call estimateFeeFromTransferrableAmount need refactoring, we should call account object
fun WalletAccount.estimateFeeFromTransferrableAmount(mbwManager: MbwManager, amount: Long): BigDecimal? {
    if (this.type == WalletAccount.Type.BCHBIP44) {
        val bip44Account = this as HDAccount
        if (bip44Account.accountType == ACCOUNT_TYPE_FROM_MASTERSEED) {
            val accountIndex = bip44Account.accountIndex
            return ExactBitcoinCashValue.from(mbwManager.getSpvBchFetcher()!!
                    .estimateFeeFromTransferrableAmount(accountIndex, amount, TransactionFee.NORMAL.name, 1.0f)).value
        } else {
            val accountGuid = this.id.toString()
            return ExactBitcoinCashValue.from(mbwManager.getSpvBchFetcher()!!
                    .estimateFeeFromTransferrableAmountUnrelatedAccount(accountGuid, amount, TransactionFee.NORMAL.name, 1.0f)).value
        }
    } else if (this.type == WalletAccount.Type.BCHSINGLEADDRESS) {
        val accountGuid = this.id.toString()
        return ExactBitcoinCashValue.from(mbwManager.getSpvBchFetcher()!!
                .estimateFeeFromTransferrableAmountUnrelatedAccount(accountGuid, amount, TransactionFee.NORMAL.name, 1.0f)).value
    }
    return BigDecimal.valueOf(0)
}
