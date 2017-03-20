package com.mrd.bitlib.crypto;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.mrd.bitlib.crypto.Gf256.Share;

public class Gf256Test {

   /**
    * Test secret shares with the empty secret
    * <p>
    * Test all possible share combinations with 0 < t <= n < = 5, that is 409
    * combinations in total
    */
   @Test
   public void exhaustiveTestEmptySecret() {
      byte[] secret = new byte[] {};
      testAllShareCombinations(secret, 5);
   }

   /**
    * Test secret shares with a short secret
    * <p>
    * Test all possible share combinations with 0 < t <= n < = 5, that is 409
    * combinations in total
    */
   @Test
   public void exhaustiveTestShortSecret() {
      byte[] secret = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
      for (int i = 1; i <= 5; i++) {
         testAllShareCombinations(secret, i);
      }
   }

   /**
    * Test secret shares with a long secret
    * <p>
    * Test all possible share combinations with 0 < t <= n < = 5, that is 409
    * combinations in total
    */
   @Test
   public void exhaustiveTestLongSecret() {
      byte[] secret = new byte[1024];
      for (int i = 0; i < secret.length; i++) {
         secret[i] = (byte) i;
      }
      testAllShareCombinations(secret, 5);
   }

   private int testAllShareCombinations(byte[] secret, int maxN) {
      int tests = 0;
      for (int n = 1; n <= maxN; n++) {
         for (int t = 1; t <= n; t++) {
            tests += testAllShareCombinations(secret, t, n);
         }
      }
      return tests;
   }

   private int testAllShareCombinations(byte[] secret, int t, int n) {
      int tests = 0;
      Gf256 gf = new Gf256();
      List<Share> shares = gf.makeShares(secret, t, n);
      List<List<Share>> shareCombinations = anyOfSharesCombination(shares, t);
      for (List<Share> combination : shareCombinations) {
         byte[] combined = gf.combineShares(combination);
         Assert.assertArrayEquals(secret, combined);
         tests++;
      }
      return tests;
   }

   /**
    * Given a list of shares and the number of shares needed, construct the list
    * containing ANY combination of possible shares
    */
   private List<List<Share>> anyOfSharesCombination(List<Share> shares, int needed) {
      List<List<Share>> combinations = new LinkedList<List<Share>>();
      if (needed == 0) {
         combinations.add(new LinkedList<Share>());
      } else {
         for (Share s : shares) {
            List<Share> copy = new LinkedList<Share>(shares);
            copy.remove(s);
            List<List<Share>> subCombinations = anyOfSharesCombination(copy, needed - 1);
            for (List<Share> subList : subCombinations) {
               subList.add(s);
               combinations.add(subList);
            }
         }
      }
      return combinations;
   }

}
