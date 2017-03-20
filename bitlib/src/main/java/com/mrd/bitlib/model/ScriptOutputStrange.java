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

package com.mrd.bitlib.model;

import java.io.Serializable;

/**
 * This class is used for output scripts that we do not understand
 */
public class ScriptOutputStrange extends ScriptOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   protected ScriptOutputStrange(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      // We cannot determine the address from scripts we do not understand
      return Address.getNullAddress(network);
   }

}
