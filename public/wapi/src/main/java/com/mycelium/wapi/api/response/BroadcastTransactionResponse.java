package com.mycelium.wapi.api.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mrd.bitlib.util.Sha256Hash;

public class BroadcastTransactionResponse implements Serializable {
   private static final long serialVersionUID = 1L;

   /**
    * If true the transaction was accepted by the network. If false the
    * transaction was rejected. Possible causes include: transaction was
    * malformed, transaction spends already spent outputs (double spend)
    */
   @JsonProperty
   public final boolean success;

   /**
    * The ID of the transaction
    */
   @JsonProperty
   public final Sha256Hash txid;

   public BroadcastTransactionResponse(@JsonProperty("success") boolean success, @JsonProperty("txid") Sha256Hash txid) {
      this.success = success;
      this.txid = txid;
   }

}
