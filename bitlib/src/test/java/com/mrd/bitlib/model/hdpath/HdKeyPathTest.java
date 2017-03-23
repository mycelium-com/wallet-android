package com.mrd.bitlib.model.hdpath;

import com.google.common.primitives.UnsignedInteger;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HdKeyPathTest {
   @Test
   public void testGetChild() throws Exception {
      // m / purpose' / coin_type' / account' / change / address_index
      HdKeyPath t1 = HdKeyPath.valueOf("m/44'/0'/1'/0/3");
      assertTrue( t1 instanceof Bip44Address );
      assertTrue( !((Bip44Address) t1).isTestnet()  );
      assertTrue( ((Bip44Address) t1).isExternal()  );
      assertTrue(!t1.isHardened());

      HdKeyPath t2 = HdKeyPath.valueOf("m/44'/1'/0'/1/3");
      assertTrue( t2 instanceof Bip44Address );
      assertTrue( ((Bip44Address) t2).isTestnet()  );
      assertTrue( !((Bip44Address) t2).isExternal()  );

      HdKeyPath t3 =
            HdKeyPath
            .BIP44
            .getCoinTypeBitcoin()
            .getAccount(UnsignedInteger.valueOf(0))
            .getInternalChain()
            .getAddress(UnsignedInteger.valueOf(5));

      assertTrue(t3 != null);
      assertTrue( !((Bip44Address) t3).isExternal());
      assertTrue( !((Bip44Address) t3).isTestnet());
   }
}