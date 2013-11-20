/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.mbwapi.impl;

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.IndependentTransactionOutput;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.mbwapi.api.GetTransactionDataRequest;
import com.mrd.mbwapi.api.GetTransactionDataResponse;
import com.mrd.mbwapi.api.QueryAddressSetStatusRequest;
import com.mrd.mbwapi.api.QueryAddressSetStatusResponse;

public class UnspentCoinbaseTest {
   // first block is unspendable thus we test for the second block ever mined.
   // if satoshi rises, this test will fail
   public static final Sha256Hash COINBASE_HASH = new Sha256Hash(
         HexUtils.toBytes("0e3e2357e806b6cdb1f70b54c3a3a17b6714ee1f0e68bebb44a74b1efd512098")); // second
                                                                                                // coindbase
                                                                                                // txhash

   @Test
   public void testQueryUnspentOutputs() throws Exception {
      MyceliumWalletApiImpl.HttpEndpoint endpoint = new MyceliumWalletApiImpl.HttpEndpoint(
            "http://mws1.mycelium.com/mws");
      MyceliumWalletApiImpl api = new MyceliumWalletApiImpl(new MyceliumWalletApiImpl.HttpEndpoint[] { endpoint },
            NetworkParameters.productionNetwork);
      Address miningAddress = Address.fromString("12c6DSiU4Rq3P4ZxziKxzrL5LmMBrzjrJX"); // second
                                                                                        // address
                                                                                        // in
                                                                                        // blockchain.
      QueryAddressSetStatusResponse inv;
      inv = api.queryActiveOutputsInventory(new QueryAddressSetStatusRequest(miningAddress));
      List<OutPoint> outPointsToGet = new LinkedList<OutPoint>(inv.addressInfo.get(0).confirmed);
      GetTransactionDataResponse data;
      data = api.getTransactionData(new GetTransactionDataRequest(outPointsToGet, new LinkedList<OutPoint>(),
            new LinkedList<Sha256Hash>()));
      // Search for a particular output which is a coinbase transaction output
      boolean found = false;
      for (IndependentTransactionOutput output : data.outputs) {
         if (output.outPoint.hash.equals(COINBASE_HASH) && output.isCoinbase) {
            found = true;
            break;
         }
      }
      assertTrue("Satoshi has risen, his coins were spent", found); // this is a
                                                                    // mining
                                                                    // transaction,
                                                                    // so it
                                                                    // should
                                                                    // pass.
   }
}
