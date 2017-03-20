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

package com.mycelium.lt;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class InputValidatorTest {

   @Test
   public void testIllegalUsernames() {
      assertInvalid("abcabcabcabcabcabcabcabcabcabcabc");
      assertInvalid(" abc ");
      assertInvalid(" abc");
      assertInvalid("abc ");
      assertInvalid("a*-bc ");
      assertInvalid("Björn");
      assertInvalid("a");
      assertInvalid("_12345678901234567890");
      assertInvalid("***");
   }

   @Test
   public void testValidUsernames() {
      assertValid("abc");
      assertValid("abc abc");
      assertValid("a.-_bc0");
      assertValid("1234567890ABc");
      assertValid("ap1980");

   }

   private void assertInvalid(String input) {
      assertFalse("this should be invalid: '" + input + "'", InputValidator.isValidTraderName(input));
   }

   private void assertValid(String input) {
      assertTrue("this should be valid: '" + input + "'", InputValidator.isValidTraderName(input));
   }
}
