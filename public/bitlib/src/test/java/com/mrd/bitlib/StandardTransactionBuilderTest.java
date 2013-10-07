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
