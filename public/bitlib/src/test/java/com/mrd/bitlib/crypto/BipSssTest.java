package com.mrd.bitlib.crypto;

import com.google.bitcoinj.Base58;
import com.mrd.bitlib.crypto.BipSss.Share;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class BipSssTest {


   /**
    * Secret: (empty)
    * 2 of 2 encoding; share set ID 5df6; share length 15
    */
   @Test
   public void tvTest1() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      Collection<Share> shares = addShareWithId("SSS-4EtJVAbu6g9", "5df6", new ArrayList<Share>());
      addShareWithId("SSS-4EtJVHFLUQ7", "5df6", shares);
      String result = BipSss.combine(shares);
      assertResult(result, new byte[]{});
   }

   /**
    * Secret: 2a
    * 2 of 3 encoding; share set ID ff12; share length 16
    */
   @Test
   public void tvTest2() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      // Add all shares
      Collection<Share> shares = addShareWithId("SSS-FmEQ1wApUkUA", "ff12", new ArrayList<Share>());
      addShareWithId("SSS-FmEQ2TkajyyQ", "ff12", shares);
      addShareWithId("SSS-FmEQ2yQJjYme", "ff12", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("2a"));

      // Add 2 shares
      shares = addShareWithId("SSS-FmEQ1wApUkUA", "ff12", new ArrayList<Share>());
      addShareWithId("SSS-FmEQ2TkajyyQ", "ff12", shares);
      String result2 = BipSss.combine(shares);
      Assert.assertEquals(result2, result1);
   }

   /**
    * Secret: 0102030405
    * 1 of 1 encoding; share set ID 1234; share length 22
    */
   @Test
   public void tvTest3() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      String result = BipSss.combine(addShareWithId("SSS-2b8cxNvVnwSHzDKPr3", "1234", new ArrayList<Share>()));
      assertResult(result, HexUtils.toBytes("0102030405"));
   }

   /**
    * Secret: 0102030405
    * 5 of 8 encoding; share set ID 5851; share length 22
    */
   @Test
   public void tvTest4() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      // Add all shares
      Collection<Share> shares = addShareWithId("SSS-2cTMvHxbT71LzEzbfi", "5851", new ArrayList<Share>());
      addShareWithId("SSS-2cTMvNxxgQ3Hcg1Bab", "5851", shares);
      addShareWithId("SSS-2cTMvRAx1uoJ6T9Fz5", "5851", shares);
      addShareWithId("SSS-2cTMvSXoCxpjcMFhvb", "5851", shares);
      addShareWithId("SSS-2cTMvVpRNubeWKfbKS", "5851", shares);
      addShareWithId("SSS-2cTMvaEr6YefknXEV4", "5851", shares);
      addShareWithId("SSS-2cTMvdkULmYJVobgaW", "5851", shares);
      addShareWithId("SSS-2cTMvh5uxQ4SavKAkh", "5851", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("0102030405"));

      // Add 5 shares
      shares = addShareWithId("SSS-2cTMvHxbT71LzEzbfi", "5851", new ArrayList<Share>());
      addShareWithId("SSS-2cTMvVpRNubeWKfbKS", "5851", shares);
      addShareWithId("SSS-2cTMvaEr6YefknXEV4", "5851", shares);
      addShareWithId("SSS-2cTMvdkULmYJVobgaW", "5851", shares);
      addShareWithId("SSS-2cTMvh5uxQ4SavKAkh", "5851", shares);
      String result2 = BipSss.combine(shares);
      Assert.assertEquals(result1, result2);
   }

   /**
    * Private key: 5KG12Hn1g33JEFwdFsbjW4Hzi2fqdsEKZTtcJ3q9L6QFLvL1UJS
    * Secret: 80be1583452771c1def6789be9ab5086bf3c18dd47aa99d785056ba330bcda7aaf
    * 2 of 3 encoding; share set ID 20ba; share length 60
    */
   @Test
   public void tvTest5() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      // Add all 3 shares
      Collection<Share> shares = addShareWithId("SSS-5CJkUwdiUPZi2R8RJJzkUFvs1TWC22JAQD2T3QMyhuAvDgzrXKuhT5at", "20ba", new ArrayList<Share>());
      addShareWithId("SSS-5CJkUyu8LAq7Newbgpc58SKsuNXvQyxAtnYzVjU1bRhF5hFYyvYaKToq", "20ba", shares);
      addShareWithId("SSS-5CJkVAkE319sk7FZVnoUgaqge6vmK1bLXwN2mm9d3VgM5hzm6qdh5TrX", "20ba", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("80be1583452771c1def6789be9ab5086bf3c18dd47aa99d785056ba330bcda7aaf"));
      Assert.assertEquals(result1, "5KG12Hn1g33JEFwdFsbjW4Hzi2fqdsEKZTtcJ3q9L6QFLvL1UJS");

      // Add 2 shares
      String result2 = BipSss.combine(addShareWithId("SSS-5CJkVAkE319sk7FZVnoUgaqge6vmK1bLXwN2mm9d3VgM5hzm6qdh5TrX", "20ba", addShareWithId("SSS-5CJkUyu8LAq7Newbgpc58SKsuNXvQyxAtnYzVjU1bRhF5hFYyvYaKToq", "20ba", new ArrayList<Share>())));
      Assert.assertEquals(result1, result2);
   }

   /**
    * Private key: L2AW1Gz2962jcDY5gY1xjuep3fEbkpr3pLody77hUVm3x2MsTBPw
    * Secret: 809389201df51c18a6c81b1a5525189a20cb58ddcffbd8d28b6b30dcc43a082fb101
    * 4 of 4 encoding; share set ID bf09; share length 61
    */
   @Test
   public void tvTest6() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      Collection<Share> shares = addShareWithId("SSS-L7d2zGTyrAkwk2BESS1ksfA3ejFHhyBrPUp8M3gTgzj6XH7r7rTvEZkks", "bf09", new ArrayList<Share>());
      addShareWithId("SSS-L7d2zWZue1HrHVYfbVxoqZYXpfW2W3LN3nySsNSCzHGf1Kzd7UuGyBoKk", "bf09", shares);
      addShareWithId("SSS-L7d31V9hWyRDRi37KMA8tR6ujJJRVcbvwaANiTNSMykkki8ymQfznBE7E", "bf09", shares);
      addShareWithId("SSS-L7d32LG8vunr4bPjNkp1gwjZ4MxdgL7MLPpg87irEh4TQShCR5QFshCx2", "bf09", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("809389201df51c18a6c81b1a5525189a20cb58ddcffbd8d28b6b30dcc43a082fb101"));
      Assert.assertEquals(result1, "L2AW1Gz2962jcDY5gY1xjuep3fEbkpr3pLody77hUVm3x2MsTBPw");
   }

   /**
    * Secret: 7f34fc0d96fe07c01e28b183a5711805
    * 16 of 16 encoding; share set ID 6b91; share length 37
    */
   @Test
   public void tvTest7() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      Collection<Share> shares = addShareWithId("SSS-2mghkBRhDnYVjuy9JkUN9SMWEQfFeizsE", "6b91", new ArrayList<Share>());
      addShareWithId("SSS-2mghkFR2Fm7psBGPYC5EAUSG12QjgytWq", "6b91", shares);
      addShareWithId("SSS-2mghkHpD5x8D6t1CitLCgNxPzRxna7Dy3", "6b91", shares);
      addShareWithId("SSS-2mghkQUfD3xh51X8ZKWKG5kfD5UGtB3YW", "6b91", shares);
      addShareWithId("SSS-2mghkTDKSbL766zz4GDLHiK1LdQR5hQYo", "6b91", shares);
      addShareWithId("SSS-2mghkUTqvgNqEPVMnVP7eNT5UfJ39GVv2", "6b91", shares);
      addShareWithId("SSS-2mghkZgCcPUhDY9wagDry9kF32fEkYDdN", "6b91", shares);
      addShareWithId("SSS-2mghkbtcpPWx6Y2g4k7t8feZ2UX71AAUN", "6b91", shares);
      addShareWithId("SSS-2mghkhWxkj6oBWvQwXpXQVaDEZzCefrn7", "6b91", shares);
      addShareWithId("SSS-2mghkmwiADHSYZwokYBMEtkJRm53xUPcH", "6b91", shares);
      addShareWithId("SSS-2mghkqSiSaiPMp9R81A7yCeojoQnhbkuK", "6b91", shares);
      addShareWithId("SSS-2mghktFpVoXVKVLLrDVNRryYQwxJEzMKX", "6b91", shares);
      addShareWithId("SSS-2mghkuYfqfAheZu6ywXfGoFmwxnqw64yw", "6b91", shares);
      addShareWithId("SSS-2mghkzvNoX6AupKpt9n9PVRMgZ6FPUSLe", "6b91", shares);
      addShareWithId("SSS-2mghm2UGDwYuVYTSx9YYiYDCzeCmS2XGH", "6b91", shares);
      addShareWithId("SSS-2mghm6ePLVPJQhsryzUfst9RWochCXK5j", "6b91", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("7f34fc0d96fe07c01e28b183a5711805"));
   }

   /**
    * Secret: c71da0d239ee672c76b7ff8dec1c6b1b39b839fd22cde8732f0fa7e70f059ad2
    * 3 of 6 encoding; share set ID a53f; share length 58
    */
   @Test
   public void tvTest8() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      Collection<Share> shares = addShareWithId("SSS-ycigiEv2xRyozNqze5bxgRJZ57wCDEZrU6LJ3v4d2BWQWysFQNZ53D", "a53f", new ArrayList<Share>());
      addShareWithId("SSS-ycigjnVFuh8u794S3f3f7EZoRgj2dqCmYpscV5dA8z5wJNB8arsZv6", "a53f", shares);
      addShareWithId("SSS-ycigoVLiJH82E8kyVkYvWQFxqTN6RDXyDjnKzhpNx4y6nXVTYyQCZF", "a53f", shares);
      addShareWithId("SSS-ycigq46qLhY1bqnNvd3AASmeFNVuoFn8qZqhCcZZiNysdwRTLu2sYN", "a53f", shares);
      addShareWithId("SSS-ycigqxtDnEmRpMsaXmnXijcUB4UaLVU8H5FppscnFX81sSFACHYvv6", "a53f", shares);
      addShareWithId("SSS-ycigsaS8umN5j1d8gXzaGrEysv68oqSopwd1sXA9VsW9FjfWbhJDd9", "a53f", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("c71da0d239ee672c76b7ff8dec1c6b1b39b839fd22cde8732f0fa7e70f059ad2"));
   }


   /**
    * Secret: ea1bef413e406b7a39280a39bf8ea76b59a4543f3f1797cfb90d33492b3eb57cf05c9cbce61ecff3854028c045049cdf0ba97cd18cbfa76b58481a17ff19ca87
    * 2 of 4 encoding; share set ID e7bc; share length 102
    */
   @Test
   public void tvTest9() throws BipSss.NotEnoughSharesException, BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      Collection<Share> shares = addShareWithId("SSS-J3N84hNNoV5ZYcjnWXeRLZ4sJEmGQ7tdUfXxhasAEb2synv2Ygb1p9sW31svpMDrXM7hxYCDTSftZJdb2EtR1gQviu1NJQNV1N", "e7bc", new ArrayList<Share>());
      addShareWithId("SSS-J3N85PjRCeo4smn5KW7YJNtrrDxGXmrh58NntoxcJtuuS5wA77JycgvLx2DFfkTAbdzMapkHwSE4daS7Drs4a3NLRuNwKopn1T", "e7bc", shares);
      addShareWithId("SSS-J3N85nJxNNCfhVU87qWbh6F7tYDWHM1RpKMy6xYUFJXZA3WZkTjjbwtakws6mbQ5EYW3kddvYPcGeyGz9h3ei6J4jMZ542kwzA", "e7bc", shares);
      addShareWithId("SSS-J3N86LXy38QWEoneDmowUeX4XRycx2uQqT8jT3H4cZLMQJAi1vejjMpJiGiFnY8ahKGQncL7KC8Ko8iWHuYfQmDmfg9QYtzhnf", "e7bc", shares);
      String result1 = BipSss.combine(shares);
      assertResult(result1, HexUtils.toBytes("ea1bef413e406b7a39280a39bf8ea76b59a4543f3f1797cfb90d33492b3eb57cf05c9cbce61ecff3854028c045049cdf0ba97cd18cbfa76b58481a17ff19ca87"));
   }

   @Test
   public void notEnoughShares() throws BipSss.InvalidContentTypeException, BipSss.IncompatibleSharesException {
      Collection<Share> shares = addShareWithId("SSS-5CJkUwdiUPZi2R8RJJzkUFvs1TWC22JAQD2T3QMyhuAvDgzrXKuhT5at", "20ba", new ArrayList<Share>());
      try {
         BipSss.combine(shares);
         Assert.fail();
      } catch (BipSss.NotEnoughSharesException e) {
         // expected
      }
   }

   @Test
   public void IncompatibleShares() throws BipSss.InvalidContentTypeException, BipSss.NotEnoughSharesException {
      Collection<Share> shares = addShareWithId("SSS-5CJkUwdiUPZi2R8RJJzkUFvs1TWC22JAQD2T3QMyhuAvDgzrXKuhT5at", "20ba", new ArrayList<Share>());
      addShareWithId("SSS-J3N86LXy38QWEoneDmowUeX4XRycx2uQqT8jT3H4cZLMQJAi1vejjMpJiGiFnY8ahKGQncL7KC8Ko8iWHuYfQmDmfg9QYtzhnf", "e7bc", shares);
      try {
         BipSss.combine(shares);
         Assert.fail();
      } catch (BipSss.IncompatibleSharesException e) {
         // expected
      }
   }


   private Collection<Share> addShareWithId(String shareString, String shareId, Collection<Share> shares) {
      Share share = BipSss.Share.fromString(shareString);
      Assert.assertTrue(BitUtils.areEqual(share.id, HexUtils.toBytes(shareId)));
      shares.add(share);
      return shares;
   }

   private void assertResult(String base58Result, byte[] expected) {
      byte[] result = Base58.decodeChecked(base58Result);
      Assert.assertNotNull(result);
      Assert.assertTrue(BitUtils.areEqual(result, expected));
   }

}
