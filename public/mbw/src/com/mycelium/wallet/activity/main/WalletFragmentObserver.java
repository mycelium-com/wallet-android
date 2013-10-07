package com.mycelium.wallet.activity.main;

import java.util.Map;

import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;

public interface WalletFragmentObserver {
   public void walletChanged(Wallet wallet);

   public void balanceUpdateStarted();

   public void balanceUpdateStopped();

   public void balanceChanged(BalanceInfo info);

   public void transactionHistoryUpdateStarted();

   public void transactionHistoryUpdateStopped();

   public void transactionHistoryChanged();

   public void invoiceMapChanged(Map<String, String> invoiceMap);

   public void newExchangeRate(Double oneBtcInFiat);
}
