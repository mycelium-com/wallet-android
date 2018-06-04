/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrd.bitlib;

import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.model.UnspentTransactionOutput;

public class TransactionUtils {
   /**
    * The miner fee per 1000 bytes used by Bitcoin Core 0.9.x for block inclusion. Still valid 0.14
    */
   public static final long INCLUDE_IN_BLOCK_FEE = 1000; // 0.00001

   /**
    * The minimum fee (except for high priority transactions) to pay per 1000 bytes
    */
   public static final long MINIMUM_KB_FEE = INCLUDE_IN_BLOCK_FEE;

   /**
    * The default fee which we base our fee calculation on
    */
   public static final long DEFAULT_KB_FEE = 100000; // 0.001

   /**
    * The minimum output value allowed when relaying transactions
    */
   public static final long MINIMUM_OUTPUT_VALUE = 547;

   /**
    * Determine whether a transaction has sufficient fees according to default miner settings
    * @param tx
    *           the transaction to calculate the priority for
    * @param funding
    *           the unspent outputs that fund the transaction
    */
   public static boolean hasInSufficientFees(Transaction tx, UnspentTransactionOutput[] funding, long minerFeeToUse) {
      int txSize = tx.toBytes().length;
      // A fee is required, does it pay enough fees?
      return calculateFeePaid(tx, funding) < calculateFeeRequired(txSize, minerFeeToUse);
   }

   public static long calculateFeeRequired(int txSize, long minerFeeToUse) {
      return (1 + (txSize / 1000)) * minerFeeToUse;
   }

   private static long calculateFeePaid(Transaction tx, UnspentTransactionOutput[] funding) {
      long fee = 0;
      for (UnspentTransactionOutput in : funding) {
         fee += in.value;
      }
      for (TransactionOutput out : tx.outputs) {
         fee -= out.value;
      }
      return fee;
   }
}
