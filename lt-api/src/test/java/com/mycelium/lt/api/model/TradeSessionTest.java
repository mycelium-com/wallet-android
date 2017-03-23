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

package com.mycelium.lt.api.model;

import org.junit.Test;

import com.mrd.bitlib.TransactionUtils;

import static org.junit.Assert.fail;

public class TradeSessionTest {
   @Test
   public void testCommissionHardLimit() {
      tryCreateTradeSession(104000000, 100000000, false, "4% commission should not fail");
      tryCreateTradeSession(106000000, 100000000, true, "6% commission should fail");
   }

   @Test
   public void testCommissionMinimum() {
      tryCreateTradeSession(100000000, 100000000, false, "0 commission should not fail");
      tryCreateTradeSession(100000000 + TransactionUtils.MINIMUM_OUTPUT_VALUE, 100000000, false,
            "minimum output value commission should not fail");
      tryCreateTradeSession(100000000 + TransactionUtils.MINIMUM_OUTPUT_VALUE - 1, 100000000, true,
            "less than minimum output value commission should fail");
      tryCreateTradeSession(100000000 + TransactionUtils.MINIMUM_OUTPUT_VALUE + 1, 100000000, false,
            "larger than minimum output value commission should not fail");
   }

   @Test
   public void testSendReceive() {
      tryCreateTradeSession(TransactionUtils.MINIMUM_OUTPUT_VALUE -1, TransactionUtils.MINIMUM_OUTPUT_VALUE -1, true, "buyer received less than minimum allowed output should fail");
      tryCreateTradeSession(100000000, 100000001, true, "buyer receives more than what the seller sends should fail");
      tryCreateTradeSession(100000000, 0, true, "buyer receives zero should fail");
      tryCreateTradeSession(100000000, -1, true, "buyer receives negative should fail");
      tryCreateTradeSession(-1, 100000000, true, "sender sends negative");
   }

   private void tryCreateTradeSession(long satoshisFromSeller, long satoshisForBuyer, boolean shouldFail, String message) {
      try {
         new TradeSession(null, 0, 0, null, 0, null, 0, 0, satoshisFromSeller, satoshisForBuyer, null, null, null,
               null, null, null, null, null, null, false, true, null, 0D, null, null, null, null, null, null, false,
               false, null);
         if (shouldFail) {
            fail(message);
         }
      } catch (Exception e) {
         if (!shouldFail) {
            fail(message);
         }
      }
   }
}
