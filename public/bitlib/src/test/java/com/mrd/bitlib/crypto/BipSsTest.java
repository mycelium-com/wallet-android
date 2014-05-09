package com.mrd.bitlib.crypto;

import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.mrd.bitlib.crypto.BipSs.EncodingFormat;
import com.mrd.bitlib.crypto.BipSs.IncompatibleSharesException;
import com.mrd.bitlib.crypto.BipSs.InvalidContentTypeException;
import com.mrd.bitlib.crypto.BipSs.NotEnoughSharesException;
import com.mrd.bitlib.crypto.BipSs.Share;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;

public class BipSsTest {

   private static final String TV1_PK = "5KdGxvP4QWyAcMBaqaixTrksuLFSBaDCRUCv2sykzNBqzSJQmku";
   private static final String TV1_S1 = "SKMaGkFbPWiyve22b3oBcBZSNbNWu9qzqfKZWuBgn7sCZgFsqizNq";
   private static final String TV1_S2 = "SKMcsd7jgD5MJk2YSzNpyDnugi2hqcLghhPnbhvDbWECzNfkJNc53";
   private static final String TV1_S3 = "SKMfG8ZUkKKT6VJDVDmpUjjNJMqtLE5csjaauxUhb9zo7XdoWDqzP";

   private static final String TV2_PK = "cVxhkPaB89GWJn65M3RtsFK6wCG7sTcJ5LnW2ZMUcCU2zxr24iQ7";
   private static final String TV2_S1 = "SLgoCtUFveresUizAcLqvhL3bwqQ7hudsSkUY1wG4vn2gfvcPgiXx";
   private static final String TV2_S2 = "SLgqcW4h8bEeFgLRH2CQrWSc5KR5SbLX5nR8Q2VyDgjtrKNMpjc7m";
   private static final String TV2_S3 = "SLgrwc4QY3P6qSPcrfzXEzq4kvekYMQhZLyk1Bcw73SfSG5hpnVf5";

   private static final String TV3_PK = "5JuuXb7tQNk1Nmme7Nqs3Pe3o7iSECzdRDmEb5U8ekHazrTnB3u";
   private static final String TV3_S1 = "SKFd7FtmvyZLH3VRLMUjdkpQRi5DBpYiEDWaGt2tigybUusNANSBpT76o";
   private static final String TV3_S2 = "SKFd7Ftq4EgoevCaicMcCRrhgBP2wiqUE6hresfHN5gZqBAeqSVxUqjHE";

   private static final String TV4_SEED = "bd6b8f33bad1d3314f040a49aea3613e";
   private static final String TV4_S1 = "SS9yu27afjaGGfCtGYrvojo8HmxhWHW";
   private static final String TV4_S2 = "SS9zmdhsMcSm7YFzGnMxykYLnCc5hce";
   private static final String TV4_S3 = "SSA5AKrXof6F6u1Ytv5rRWhiXaCZkBs";

   private static final InMemoryPrivateKey PPK_1 = new InMemoryPrivateKey(
         "Kyqg1PJsc5QzLC8rv5BwC156aXBiZZuEyt6FqRQRTXBjTX96bNkW", NetworkParameters.productionNetwork);
   private static final InMemoryPrivateKey PPK_2 = new InMemoryPrivateKey(
         "KyqHExGgWAkmPB4h3pk7VJWLA9nMN4jCQen1LfveZN5tyDn75dYH", NetworkParameters.productionNetwork);
   private static final InMemoryPrivateKey TPK_1 = new InMemoryPrivateKey(
         "cVxhkPaB89GWJn65M3RtsFK6wCG7sTcJ5LnW2ZMUcCU2zxr24iQ7", NetworkParameters.testNetwork);
   private static final byte[] SEED_1 = HexUtils.toBytes("4f040a49aea3613ebd6b8f33bad1d331");

   @Test
   public void tv1() throws IncompatibleSharesException, NotEnoughSharesException, InvalidContentTypeException {
      InMemoryPrivateKey pk = new InMemoryPrivateKey(TV1_PK, NetworkParameters.productionNetwork);
      List<Share> shares = BipSs.shard(pk, 2, 3, EncodingFormat.COMPACT, NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_S1, shares.get(0).toString());
      Assert.assertEquals(TV1_S2, shares.get(1).toString());
      Assert.assertEquals(TV1_S3, shares.get(2).toString());
      encodeDecodeEncode(shares);
      Assert.assertEquals(pk, BipSs.combinePrivateKey(shares));
   }

   private void encodeDecodeEncode(List<Share> shares) {
      for (Share s : shares) {
         encodeDecodeEncode(s);
      }
   }

   private void encodeDecodeEncode(Share s) {
      Assert.assertEquals(s.toString(), Share.fromString(s.toString(), s.network).toString());
   }

   @Test
   public void tv2() throws IncompatibleSharesException, NotEnoughSharesException, InvalidContentTypeException {
      InMemoryPrivateKey pk = new InMemoryPrivateKey(TV2_PK, NetworkParameters.testNetwork);
      List<Share> shares = BipSs.shard(pk, 2, 3, EncodingFormat.COMPACT, NetworkParameters.testNetwork);
      Assert.assertEquals(TV2_S1, shares.get(0).toString());
      Assert.assertEquals(TV2_S2, shares.get(1).toString());
      Assert.assertEquals(TV2_S3, shares.get(2).toString());
      encodeDecodeEncode(shares);
      Assert.assertEquals(pk, BipSs.combinePrivateKey(shares));
   }

   @Test
   public void tv3() throws IncompatibleSharesException, NotEnoughSharesException, InvalidContentTypeException {
      InMemoryPrivateKey pk = new InMemoryPrivateKey(TV3_PK, NetworkParameters.productionNetwork);
      List<Share> shares = BipSs.shard(pk, 2, 2, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      Assert.assertEquals(TV3_S1, shares.get(0).toString());
      Assert.assertEquals(TV3_S2, shares.get(1).toString());
      encodeDecodeEncode(shares);
      Assert.assertEquals(pk, BipSs.combinePrivateKey(shares));
   }

   @Test
   public void tv4() throws IncompatibleSharesException, NotEnoughSharesException, InvalidContentTypeException {
      byte[] seed = HexUtils.toBytes(TV4_SEED);
      List<Share> shares = BipSs.shard(seed, 2, 3, EncodingFormat.COMPACT, NetworkParameters.productionNetwork);
      Assert.assertEquals(TV4_S1, shares.get(0).toString());
      Assert.assertEquals(TV4_S2, shares.get(1).toString());
      Assert.assertEquals(TV4_S3, shares.get(2).toString());
      encodeDecodeEncode(shares);
      Assert.assertTrue(BitUtils.areEqual(seed, BipSs.combineSeed(shares)));
   }

   /**
    * Test that you cannot combine shares for two different keys when using LONG
    * encoding
    */
   @Test
   public void incompatibleContentHash() throws NotEnoughSharesException, InvalidContentTypeException {

      // Long encoding can tell us if the shares from two different sets are
      // incompatible
      List<Share> pk1Shares = BipSs.shard(PPK_1, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> pk2Shares = BipSs.shard(PPK_2, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(pk1Shares.get(0)));
      incompatibleShares.add(encodeDecode(pk2Shares.get(1)));
      try {
         BipSs.combinePrivateKey(incompatibleShares);
         Assert.fail("Should throw");
      } catch (IncompatibleSharesException e) {
         // Expected
      }

      // Compact encoding cannot tell us if the shares from two different sets
      // are incompatible
      pk1Shares = BipSs.shard(PPK_1, 2, 3, EncodingFormat.COMPACT, NetworkParameters.productionNetwork);
      pk2Shares = BipSs.shard(PPK_2, 2, 3, EncodingFormat.COMPACT, NetworkParameters.productionNetwork);
      incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(pk1Shares.get(0)));
      incompatibleShares.add(encodeDecode(pk2Shares.get(1)));
      try {
         BipSs.combinePrivateKey(incompatibleShares);
      } catch (IncompatibleSharesException e) {
         Assert.fail("Should throw");
      }

      // Short encoding cannot tell us if the shares from two different sets are
      // incompatible
      pk1Shares = BipSs.shard(PPK_1, 2, 3, EncodingFormat.SHORT, NetworkParameters.productionNetwork);
      pk2Shares = BipSs.shard(PPK_2, 2, 3, EncodingFormat.SHORT, NetworkParameters.productionNetwork);
      incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(pk1Shares.get(0)));
      incompatibleShares.add(encodeDecode(pk2Shares.get(1)));
      try {
         BipSs.combinePrivateKey(incompatibleShares);
      } catch (IncompatibleSharesException e) {
         Assert.fail("Should throw");
      }

   }

   /**
    * Test that you cannot combine shares from two different networks
    */
   @Test
   public void incompatibleNetworks() throws NotEnoughSharesException, InvalidContentTypeException {
      List<Share> pk1Shares = BipSs.shard(PPK_1, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> pk2Shares = BipSs.shard(TPK_1, 2, 3, EncodingFormat.LONG, NetworkParameters.testNetwork);
      List<Share> incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(pk1Shares.get(0)));
      incompatibleShares.add(encodeDecode(pk2Shares.get(1)));
      try {
         BipSs.combineSeed(incompatibleShares);
         Assert.fail("Should throw");
      } catch (IncompatibleSharesException e) {
         // Expected
      }

   }

   /**
    * Test that you cannot combine shares with two different thresholds
    */
   @Test
   public void incompatibleThresholds() throws NotEnoughSharesException, InvalidContentTypeException {
      List<Share> shares1 = BipSs.shard(PPK_1, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> shares2 = BipSs.shard(PPK_1, 3, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(shares1.get(0)));
      incompatibleShares.add(encodeDecode(shares2.get(1)));
      incompatibleShares.add(encodeDecode(shares2.get(2)));
      try {
         BipSs.combineSeed(incompatibleShares);
         Assert.fail("Should throw");
      } catch (IncompatibleSharesException e) {
         // Expected
      }

   }

   /**
    * Test that you cannot combine shares with two different content types
    */
   @Test
   public void incompatibleContentTypes() throws NotEnoughSharesException, InvalidContentTypeException {
      List<Share> shares1 = BipSs.shard(PPK_1, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> shares2 = BipSs.shard(SEED_1, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(shares1.get(0)));
      incompatibleShares.add(encodeDecode(shares2.get(1)));
      try {
         BipSs.combineSeed(incompatibleShares);
         Assert.fail("Should throw");
      } catch (IncompatibleSharesException e) {
         // Expected
      }

   }

   /**
    * Test that you cannot combine with too few shares
    */
   @Test
   public void tooFewShares() throws IncompatibleSharesException, InvalidContentTypeException {
      List<Share> shares1 = BipSs.shard(PPK_1, 2, 3, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      List<Share> incompatibleShares = new LinkedList<Share>();
      incompatibleShares.add(encodeDecode(shares1.get(0)));
      try {
         BipSs.combinePrivateKey(incompatibleShares);
         Assert.fail("Should throw");
      } catch (NotEnoughSharesException e) {
         // Expected
      }
   }

   /**
    * Test that you combine with too many shares
    */
   @Test
   public void tooManyShares() throws IncompatibleSharesException, InvalidContentTypeException,
         NotEnoughSharesException {
      List<Share> shares = BipSs.shard(PPK_1, 4, 8, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      BipSs.combinePrivateKey(shares);
   }

   /**
    * Test invalid content type
    */
   @Test
   public void invalidContentType() throws IncompatibleSharesException, NotEnoughSharesException {
      List<Share> shares = BipSs.shard(PPK_1, 2, 2, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      try {
         BipSs.combineSeed(shares);
         Assert.fail("Should throw");
      } catch (InvalidContentTypeException e) {
         // Expected
      }

      shares = BipSs.shard(SEED_1, 2, 2, EncodingFormat.LONG, NetworkParameters.productionNetwork);
      try {
         BipSs.combinePrivateKey(shares);
         Assert.fail("Should throw");
      } catch (InvalidContentTypeException e) {
         // Expected
      }
   }

   private Share encodeDecode(Share s) {
      return Share.fromString(s.toString(), s.network);
   }

}
