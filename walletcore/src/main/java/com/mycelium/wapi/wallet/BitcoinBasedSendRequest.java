package com.mycelium.wapi.wallet;

import com.mrd.bitlib.UnsignedTransaction;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

public abstract class BitcoinBasedSendRequest<T extends GenericTransaction> extends SendRequest<T> {

   private UnsignedTransaction unsignedTx;

   protected BitcoinBasedSendRequest(CryptoCurrency type, Value fee) {
      super(type, fee);
   }

   public UnsignedTransaction getUnsignedTx() {
      return unsignedTx;
   }

}
