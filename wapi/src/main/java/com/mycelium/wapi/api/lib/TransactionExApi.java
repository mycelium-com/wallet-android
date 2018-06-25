package com.mycelium.wapi.api.lib;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.TransactionEx;

public class TransactionExApi extends TransactionEx {

   /** shows if this transaction references other unconfirmed transactions
    *
    *    if this transaction is already mined -> -1
    *    if all referenced inputs are confirmed -> 0
    *    if one or more input is unconfirmed -> max(unconfirmedChainLength of all inputs) + 1
    *    -> i.e. there are at least `unconfirmedChainLength`-transactions that need to get mined until this tx can get mined.
    */
   @JsonProperty("unconf_chain")
   public final int unconfirmedChainLength; // -1 means unconfirmed

   @JsonProperty("rbf")
   public final boolean rbfRisk; // true means this tx or any unconfed parent of it is marked for RBF

   public TransactionExApi(
         @JsonProperty("txid") Sha256Hash txid,
         @JsonProperty("txhash") Sha256Hash txHash,
         @JsonProperty("height") int height,
         @JsonProperty("time") int time,
         @JsonProperty("binary") byte[] binary,
         @JsonProperty("unconf_chain") int unconfirmedChainLength,
         @JsonProperty("rbf") boolean rbfRisk) {
      super(txid, txHash, height, time, binary);
      this.unconfirmedChainLength = unconfirmedChainLength;
      this.rbfRisk = rbfRisk;
   }
}
