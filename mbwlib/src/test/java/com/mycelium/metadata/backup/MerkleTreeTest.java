package com.mycelium.metadata.backup;

import com.mrd.bitlib.util.HexUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MerkleTreeTest  {
   @Test
   public void testFromData() throws Exception {
      // test vectors from https://github.com/oleganza/bitcoin-papers/blob/master/AutomaticEncryptedWalletBackups.md#test-vectors
      byte[] cipherText = HexUtils.toBytes("5edbaade9ba4ed528a8de36c95ece996189dedf4756fba2599f94b4f370d7013" +
            "66e2f0ba4e59111c0787708cf4b0b82de558b4d8bf5d90b3512f09814d605d4c" +
            "14f2f85b596211f83918c31c4bef19ea");

      MerkleTree merkleTree = MerkleTree.fromData(cipherText);

      assertEquals("Merkle root",
            "9e913cd60f7df551b3baa320602bfba78489921d661362a64a03550a45add008",
            merkleTree.getRoot().toHex());
   }

   @Test
   public void testBigDataEven() throws Exception {
      // test vectors from https://github.com/oleganza/bitcoin-papers/blob/master/AutomaticEncryptedWalletBackups.md#test-vectors
      byte[] test = new byte[1024 * 1024];
      for (int a=0; a<test.length; a++){
         test[a]=(byte)(a % 100);
      }

      MerkleTree merkleTree = MerkleTree.fromData(test);

      assertEquals("Merkle root",
            "481ce02aff3b0c6d9f35849c8c029fa32fef1c329f42481362b7d7faf41d3f8e",
            merkleTree.getRoot().toHex());
   }

   @Test
   public void testBigDataOdd() throws Exception {
      // test vectors from https://github.com/oleganza/bitcoin-papers/blob/master/AutomaticEncryptedWalletBackups.md#test-vectors
      byte[] test = new byte[1024 * 1024 + 1];
      for (int a=0; a<test.length; a++){
         test[a]=(byte)(a % 100);
      }

      MerkleTree merkleTree = MerkleTree.fromData(test);

      assertEquals("Merkle root",
            "74215dd802f079ce1af9ebcca049508bf592eafc83ddbe14483bbfb1501a5628",
            merkleTree.getRoot().toHex());
   }

   @Test
   public void testBigDataSmallChunks() throws Exception {
      // test vectors from https://github.com/oleganza/bitcoin-papers/blob/master/AutomaticEncryptedWalletBackups.md#test-vectors
      byte[] test = new byte[1024 * 1024 + 1];
      for (int a=0; a<test.length; a++){
         test[a]=(byte)(a % 100);
      }

      MerkleTree merkleTree = MerkleTree.fromData(test, 10);

      assertEquals("Merkle root",
            "8d93a23d2c9ed526ce1100a9e3b8b979c2057526abfe2cb89c3f2614345b2a98",
            merkleTree.getRoot().toHex());
   }
}