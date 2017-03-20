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

package com.mrd.bitlib;

import com.google.common.collect.ImmutableList;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.ScriptOutputStandard;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.bitlib.util.Sha256Hash;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * a programmer
 */
public class StandardTransactionBuilderTest {

   private NetworkParameters network;
   private StandardTransactionBuilder testme;

   @Before
   public void setUp() throws Exception {
      network = NetworkParameters.testNetwork;
      testme = new StandardTransactionBuilder(network);
   }

   @Test   (expected = IllegalArgumentException.class)
   public void testEmptyList() throws Exception {
      testme.extractRichest(ImmutableList.<UnspentTransactionOutput>of(), network);
   }

   @Test
   public void testSingleList() throws Exception {
      Address addr = Address.fromString("mfx7u4LpuqG5CA5NFZBG3U1UTmftKXHzzk");
      UnspentTransactionOutput output = new UnspentTransactionOutput(new OutPoint(Sha256Hash.ZERO_HASH, 0), 0, 100, new ScriptOutputStandard(addr.getTypeSpecificBytes()));
      Address address = testme.extractRichest(ImmutableList.of(output), network);
      assertEquals(addr,address);
   }

   @Test
   public void testThreeElemList() throws Exception {
      Address addr1 = Address.fromString("mfx7u4LpuqG5CA5NFZBG3U1UTmftKXHzzk");
      Address addr2 = Address.fromString("mnZj5DJuSNbc3wppJnbihnsyq6mfWfnTrT");
      UnspentTransactionOutput output1 = new UnspentTransactionOutput(new OutPoint(Sha256Hash.ZERO_HASH, 0), 0, 50, new ScriptOutputStandard(addr1.getTypeSpecificBytes()));
      UnspentTransactionOutput output2 = new UnspentTransactionOutput(new OutPoint(Sha256Hash.ZERO_HASH, 0), 0, 50, new ScriptOutputStandard(addr1.getTypeSpecificBytes()));
      UnspentTransactionOutput output3 = new UnspentTransactionOutput(new OutPoint(Sha256Hash.ZERO_HASH, 0), 0, 101, new ScriptOutputStandard(addr2.getTypeSpecificBytes()));
      Address address = testme.extractRichest(ImmutableList.of(output1,output2,output3), network);
      assertEquals(addr2,address);
   }


}
