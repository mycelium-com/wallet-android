package com.mycelium.wallet.colu;

import com.coinapult.api.httpclient.Transaction;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

//TODO: do we need this for Colu ?
public class ColuTransactionSummary extends TransactionSummary {

   public final Transaction.Json input;

   public ColuTransactionSummary(Optional<Address> address, CurrencyValue value, boolean isIncoming, Transaction.Json input) {
      super(getTxid(input), value, isIncoming ,showTime(input), -1, confs(input), false, null, address, null);
      this.input = input;
   }

   private static Sha256Hash getTxid(Transaction.Json input) {
      //since this is not a blockchain tx, we use a hash of the 24 bytes as a surrogate id.
      return HashUtils.doubleSha256(HexUtils.toBytes(input.tid));
   }

   private static int confs(Transaction.Json input) {
      //set to 7 confirmations because that way it will show as "confirmed" instead of showing the number
      return isConfirmed(input) ? 7 : 0;
   }

   private static long showTime(Transaction.Json input) {
      // if the the tx is not confirmed, use the timestamp (last fetch from server) as time
      return isConfirmed(input) ? input.completeTime : input.timestamp;
   }

   private static boolean isConfirmed(Transaction.Json input) {
      return input.state.equals("complete");
   }

   @Override
   public boolean hasDetails() {
      return false;
   }
/* TODO: replace with Colu ?
   @Override
   public boolean canCoinapult() {
      return true;
   }
*/
   @Override
   public boolean canCancel() {
      return false;
   }
}
