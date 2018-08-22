package com.mrd.bitlib.model.hdpath;

import com.google.common.primitives.UnsignedInteger;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HdKeyPathTest {
   @Test
   public void testGetChild() throws Exception {
      // m / purpose' / coin_type' / account' / change / address_index
      HdKeyPath t1 = HdKeyPath.valueOf("m/44'/0'/1'/0/3");
      assertTrue( t1 instanceof Bip44Address );
      assertFalse(((Bip44CoinType) t1.getParent().getParent().getParent()).isTestnet());
      assertTrue(((Bip44Chain) t1.getParent()).isExternal());
      assertFalse(t1.isHardened());

      HdKeyPath t2 = HdKeyPath.valueOf("m/44'/1'/0'/1/3");
      assertTrue( t2 instanceof Bip44Address );
      assertTrue(((Bip44CoinType) t2.getParent().getParent().getParent()).isTestnet());
      assertFalse(((Bip44Chain) t2.getParent()).isExternal());

      HdKeyPath t3 =
            HdKeyPath
            .BIP44
            .getCoinTypeBitcoin()
            .getAccount(UnsignedInteger.valueOf(0))
            .getInternalChain()
            .getAddress(UnsignedInteger.valueOf(5));

      assertNotNull(t3);
      assertFalse(((Bip44CoinType) t3.getParent().getParent().getParent()).isTestnet());
      assertFalse(((Bip44Chain) t3.getParent()).isExternal());

      HdKeyPath t4 =
            HdKeyPath
            .BIP49
            .getCoinTypeBitcoin()
            .getAccount(UnsignedInteger.valueOf(0))
            .getInternalChain()
            .getAddress(UnsignedInteger.valueOf(5));

      assertNotNull(t4);
      assertFalse(((Bip44CoinType) t4.getParent().getParent().getParent()).isTestnet());
      assertFalse(((Bip44Chain) t4.getParent()).isExternal());

      HdKeyPath t5 =
            HdKeyPath
            .BIP84
            .getCoinTypeBitcoin()
            .getAccount(UnsignedInteger.valueOf(0))
            .getInternalChain()
            .getAddress(UnsignedInteger.valueOf(5));

      assertNotNull(t5);
      assertFalse(((Bip44CoinType) t5.getParent().getParent().getParent()).isTestnet());
      assertFalse(((Bip44Chain) t5.getParent()).isExternal());
   }
}