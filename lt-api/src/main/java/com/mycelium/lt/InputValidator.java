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

import com.google.common.base.CharMatcher;
import static com.google.common.base.CharMatcher.*;

public class InputValidator {
   private static final int MAXIMUM_TRADER_NAME_LENGTH = 20;
   private static final int MINIMUM_TRADER_NAME_LENGTH = 3;
   private static final CharMatcher VALID_USER_CHARS =
         inRange('a', 'z')
               .or(inRange('A', 'Z'))
               .or(DIGIT)
               .or(WHITESPACE)
               .or(anyOf("._-"));


   public static boolean isValidTraderName(String name) {
      // Allow a-z, A-Z, 0-9, dot, underbar, and dash
      return name != null && name.length() >= MINIMUM_TRADER_NAME_LENGTH && name.length() <= MAXIMUM_TRADER_NAME_LENGTH
            && WHITESPACE.trimFrom(name).equals(name) // must not
            // start or
            // end with
            // whitespace
            && VALID_USER_CHARS.matchesAllOf(name);
   }
}
