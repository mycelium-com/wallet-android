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

package com.mrd.bitlib.crypto;

import com.mrd.bitlib.model.NetworkParameters;

/**
 * Allows exporting of private keys in base58 format also known as SIPA format.
 */
public interface KeyExporter {
   /**
    * Get the private key as a base-58 encoded key.
    * 
    * @param network
    *           The network parameters to use
    * @return The private key as a base-58 encoded key.
    */
   String getBase58EncodedPrivateKey(NetworkParameters network);

   /**
    * Get the private key as an array of bytes.
    * 
    * @return The bytes of the private key.
    */
   byte[] getPrivateKeyBytes();
}
