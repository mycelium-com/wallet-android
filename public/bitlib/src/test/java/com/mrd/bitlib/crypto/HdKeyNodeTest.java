package com.mrd.bitlib.crypto;

import junit.framework.Assert;

import org.junit.Test;

import com.mrd.bitlib.crypto.HdKeyNode.KeyGenerationException;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;

public class HdKeyNodeTest {

   private static final byte[] TV1_MASTER_SEED = HexUtils.toBytes("000102030405060708090a0b0c0d0e0f");
   private static final String TV1_TEST_M_PUB = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8";
   private static final String TV1_TEST_M_PRV = "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi";
   private static final String TV1_TEST_M_0H_PUB = "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8PX9rL2dZXvgGDnw";
   private static final String TV1_TEST_M_0H_PRV = "xprv9uHRZZhk6KAJC1avXpDAp4MDc3sQKNxDiPvvkX8Br5ngLNv1TxvUxt4cV1rGL5hj6KCesnDYUhd7oWgT11eZG7XnxHrnYeSvkzY7d2bhkJ7";
   private static final String TV1_TEST_M_0H_1_PUB = "xpub6ASuArnXKPbfEwhqN6e3mwBcDTgzisQN1wXN9BJcM47sSikHjJf3UFHKkNAWbWMiGj7Wf5uMash7SyYq527Hqck2AxYysAA7xmALppuCkwQ";
   private static final String TV1_TEST_M_0H_1_PRV = "xprv9wTYmMFdV23N2TdNG573QoEsfRrWKQgWeibmLntzniatZvR9BmLnvSxqu53Kw1UmYPxLgboyZQaXwTCg8MSY3H2EU4pWcQDnRnrVA1xe8fs";
   private static final String TV1_TEST_M_0H_1_2H_PUB = "xpub6D4BDPcP2GT577Vvch3R8wDkScZWzQzMMUm3PWbmWvVJrZwQY4VUNgqFJPMM3No2dFDFGTsxxpG5uJh7n7epu4trkrX7x7DogT5Uv6fcLW5";
   private static final String TV1_TEST_M_0H_1_2H_PRV = "xprv9z4pot5VBttmtdRTWfWQmoH1taj2axGVzFqSb8C9xaxKymcFzXBDptWmT7FwuEzG3ryjH4ktypQSAewRiNMjANTtpgP4mLTj34bhnZX7UiM";
   private static final String TV1_TEST_M_0H_1_2H_2_PUB = "xpub6FHa3pjLCk84BayeJxFW2SP4XRrFd1JYnxeLeU8EqN3vDfZmbqBqaGJAyiLjTAwm6ZLRQUMv1ZACTj37sR62cfN7fe5JnJ7dh8zL4fiyLHV";
   private static final String TV1_TEST_M_0H_1_2H_2_PRV = "xprvA2JDeKCSNNZky6uBCviVfJSKyQ1mDYahRjijr5idH2WwLsEd4Hsb2Tyh8RfQMuPh7f7RtyzTtdrbdqqsunu5Mm3wDvUAKRHSC34sJ7in334";
   private static final String TV1_TEST_M_0H_1_2H_2_1000000000_PUB = "xpub6H1LXWLaKsWFhvm6RVpEL9P4KfRZSW7abD2ttkWP3SSQvnyA8FSVqNTEcYFgJS2UaFcxupHiYkro49S8yGasTvXEYBVPamhGW6cFJodrTHy";
   private static final String TV1_TEST_M_0H_1_2H_2_1000000000_PRV = "xprvA41z7zogVVwxVSgdKUHDy1SKmdb533PjDz7J6N6mV6uS3ze1ai8FHa8kmHScGpWmj4WggLyQjgPie1rFSruoUihUZREPSL39UNdE3BBDu76";

   private static final byte[] TV2_MASTER_SEED = HexUtils
         .toBytes("fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542");
   private static final String TV2_TEST_M_PUB = "xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB";
   private static final String TV2_TEST_M_PRV = "xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U";
   private static final String TV2_TEST_M_0_PUB = "xpub69H7F5d8KSRgmmdJg2KhpAK8SR3DjMwAdkxj3ZuxV27CprR9LgpeyGmXUbC6wb7ERfvrnKZjXoUmmDznezpbZb7ap6r1D3tgFxHmwMkQTPH";
   private static final String TV2_TEST_M_0_PRV = "xprv9vHkqa6EV4sPZHYqZznhT2NPtPCjKuDKGY38FBWLvgaDx45zo9WQRUT3dKYnjwih2yJD9mkrocEZXo1ex8G81dwSM1fwqWpWkeS3v86pgKt";
   private static final String TV2_TEST_M_0_2147483647H_PUB = "xpub6ASAVgeehLbnwdqV6UKMHVzgqAG8Gr6riv3Fxxpj8ksbH9ebxaEyBLZ85ySDhKiLDBrQSARLq1uNRts8RuJiHjaDMBU4Zn9h8LZNnBC5y4a";
   private static final String TV2_TEST_M_0_2147483647H_PRV = "xprv9wSp6B7kry3Vj9m1zSnLvN3xH8RdsPP1Mh7fAaR7aRLcQMKTR2vidYEeEg2mUCTAwCd6vnxVrcjfy2kRgVsFawNzmjuHc2YmYRmagcEPdU9";
   private static final String TV2_TEST_M_0_2147483647H_1_PUB = "xpub6DF8uhdarytz3FWdA8TvFSvvAh8dP3283MY7p2V4SeE2wyWmG5mg5EwVvmdMVCQcoNJxGoWaU9DCWh89LojfZ537wTfunKau47EL2dhHKon";
   private static final String TV2_TEST_M_0_2147483647H_1_PRV = "xprv9zFnWC6h2cLgpmSA46vutJzBcfJ8yaJGg8cX1e5StJh45BBciYTRXSd25UEPVuesF9yog62tGAQtHjXajPPdbRCHuWS6T8XA2ECKADdw4Ef";
   private static final String TV2_TEST_M_0_2147483647H_1_2147483646H_PUB = "xpub6ERApfZwUNrhLCkDtcHTcxd75RbzS1ed54G1LkBUHQVHQKqhMkhgbmJbZRkrgZw4koxb5JaHWkY4ALHY2grBGRjaDMzQLcgJvLJuZZvRcEL";
   private static final String TV2_TEST_M_0_2147483647H_1_2147483646H_PRV = "xprvA1RpRA33e1JQ7ifknakTFpgNXPmW2YvmhqLQYMmrj4xJXXWYpDPS3xz7iAxn8L39njGVyuoseXzU6rcxFLJ8HFsTjSyQbLYnMpCqE2VbFWc";
   private static final String TV2_TEST_M_0_2147483647H_1_2147483646H_2_PUB = "xpub6FnCn6nSzZAw5Tw7cgR9bi15UV96gLZhjDstkXXxvCLsUXBGXPdSnLFbdpq8p9HmGsApME5hQTZ3emM2rnY5agb9rXpVGyy3bdW6EEgAtqt";
   private static final String TV2_TEST_M_0_2147483647H_1_2147483646H_2_PRV = "xprvA2nrNbFZABcdryreWet9Ea4LvTJcGsqrMzxHx98MMrotbir7yrKCEXw7nadnHM8Dq38EGfSh6dqA9QWTyefMLEcBYJUuekgW4BYPJcr9E7j";

   private static final int HARDENED_PRIVATE_KEY = 0x80000000;

   @Test
   public void tv1PrivateTest() throws KeyGenerationException {
      HdKeyNode root = HdKeyNode.fromSeed(TV1_MASTER_SEED);
      String extPub = root.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_PUB, extPub);
      String extPrv = root.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_PRV, extPrv);

      // Serialization
      Assert.assertEquals(root,
            HdKeyNode.parse(root.serialize(NetworkParameters.productionNetwork), NetworkParameters.productionNetwork));
      Assert.assertEquals(root,
            HdKeyNode.parse(root.serialize(NetworkParameters.testNetwork), NetworkParameters.testNetwork));

      HdKeyNode m_0h = root.createChildNode(0 | HARDENED_PRIVATE_KEY);
      extPub = m_0h.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_PUB, extPub);
      extPrv = m_0h.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_PRV, extPrv);

      HdKeyNode m_0h_1 = m_0h.createChildNode(1);
      extPub = m_0h_1.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_PUB, extPub);
      extPrv = m_0h_1.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_PRV, extPrv);

      HdKeyNode m_0h_1_2h = m_0h_1.createChildNode(2 | HARDENED_PRIVATE_KEY);
      extPub = m_0h_1_2h.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_PUB, extPub);
      extPrv = m_0h_1_2h.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_PRV, extPrv);

      HdKeyNode m_0h_1_2h_2 = m_0h_1_2h.createChildNode(2);
      extPub = m_0h_1_2h_2.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_2_PUB, extPub);
      extPrv = m_0h_1_2h_2.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_2_PRV, extPrv);

      HdKeyNode m_0h_1_2h_2_1000000000 = m_0h_1_2h_2.createChildNode(1000000000);
      extPub = m_0h_1_2h_2_1000000000.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_2_1000000000_PUB, extPub);
      extPrv = m_0h_1_2h_2_1000000000.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_2_1000000000_PRV, extPrv);

      // Serialization
      Assert.assertEquals(m_0h_1_2h_2_1000000000, HdKeyNode.parse(
            m_0h_1_2h_2_1000000000.serialize(NetworkParameters.productionNetwork), NetworkParameters.productionNetwork));
      Assert.assertEquals(m_0h_1_2h_2_1000000000, HdKeyNode.parse(
            m_0h_1_2h_2_1000000000.serialize(NetworkParameters.testNetwork), NetworkParameters.testNetwork));

   }

   @Test
   public void tv1PublicTest() throws KeyGenerationException {
      HdKeyNode root = HdKeyNode.fromSeed(TV1_MASTER_SEED);
      HdKeyNode rootPublic = root.getPublicNode();

      // Serialization
      Assert.assertEquals(rootPublic, HdKeyNode.parse(rootPublic.serialize(NetworkParameters.productionNetwork),
            NetworkParameters.productionNetwork));
      Assert.assertEquals(rootPublic,
            HdKeyNode.parse(rootPublic.serialize(NetworkParameters.testNetwork), NetworkParameters.testNetwork));

      String extPub = rootPublic.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_PUB, extPub);
      try {
         rootPublic.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }
      try {
         rootPublic.createChildNode(0 | HARDENED_PRIVATE_KEY);
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0h = root.createChildNode(0 | HARDENED_PRIVATE_KEY);
      HdKeyNode m_0hPublic = m_0h.getPublicNode();
      extPub = m_0hPublic.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_PUB, extPub);
      try {
         m_0hPublic.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0h_1 = m_0h.createChildNode(1);
      HdKeyNode m_0h_1Public = m_0h_1.getPublicNode();
      extPub = m_0h_1Public.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_PUB, extPub);
      try {
         m_0h_1Public.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0h_1_2h = m_0h_1.createChildNode(2 | HARDENED_PRIVATE_KEY);
      HdKeyNode m_0h_1_2hPublic = m_0h_1_2h.getPublicNode();
      extPub = m_0h_1_2hPublic.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_PUB, extPub);
      try {
         m_0h_1_2hPublic.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      // This time create from parent public node as we are no longer depending
      // on hardened nodes
      HdKeyNode m_0h_1_2h_2Public = m_0h_1_2hPublic.createChildNode(2);
      extPub = m_0h_1_2h_2Public.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_2_PUB, extPub);
      try {
         m_0h_1_2h_2Public.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      // This time create from parent public node as we are no longer depending
      // on hardened nodes
      HdKeyNode m_0h_1_2h_2_1000000000Public = m_0h_1_2h_2Public.createChildNode(1000000000);
      extPub = m_0h_1_2h_2_1000000000Public.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV1_TEST_M_0H_1_2H_2_1000000000_PUB, extPub);
      try {
         m_0h_1_2h_2_1000000000Public.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      // Serialization
      Assert.assertEquals(m_0h_1_2h_2_1000000000Public, HdKeyNode.parse(
            m_0h_1_2h_2_1000000000Public.serialize(NetworkParameters.productionNetwork),
            NetworkParameters.productionNetwork));
      Assert.assertEquals(m_0h_1_2h_2_1000000000Public, HdKeyNode.parse(
            m_0h_1_2h_2_1000000000Public.serialize(NetworkParameters.testNetwork), NetworkParameters.testNetwork));

   }

   @Test
   public void tv2TestPrivate() throws KeyGenerationException {
      HdKeyNode root = HdKeyNode.fromSeed(TV2_MASTER_SEED);
      String extPub = root.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_PUB, extPub);
      String extPrv = root.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_PRV, extPrv);

      HdKeyNode m_0 = root.createChildNode(0);
      extPub = m_0.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_PUB, extPub);
      extPrv = m_0.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_PRV, extPrv);

      HdKeyNode m_0_2147483647H = m_0.createChildNode(2147483647 | HARDENED_PRIVATE_KEY);
      extPub = m_0_2147483647H.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_PUB, extPub);
      extPrv = m_0_2147483647H.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_PRV, extPrv);

      HdKeyNode m_0_2147483647H_1 = m_0_2147483647H.createChildNode(1);
      extPub = m_0_2147483647H_1.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_PUB, extPub);
      extPrv = m_0_2147483647H_1.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_PRV, extPrv);

      HdKeyNode m_0_2147483647H_1_2147483646H = m_0_2147483647H_1.createChildNode(2147483646 | HARDENED_PRIVATE_KEY);
      extPub = m_0_2147483647H_1_2147483646H.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_2147483646H_PUB, extPub);
      extPrv = m_0_2147483647H_1_2147483646H.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_2147483646H_PRV, extPrv);

      HdKeyNode m_0_2147483647H_1_2147483646H_2 = m_0_2147483647H_1_2147483646H.createChildNode(2);
      extPub = m_0_2147483647H_1_2147483646H_2.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_2147483646H_2_PUB, extPub);
      extPrv = m_0_2147483647H_1_2147483646H_2.serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_2147483646H_2_PRV, extPrv);
   }

   @Test
   public void tv2TestPublic() throws KeyGenerationException {
      HdKeyNode root = HdKeyNode.fromSeed(TV2_MASTER_SEED);
      HdKeyNode rootPublic = root.getPublicNode();
      String extPub = rootPublic.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_PUB, extPub);
      try {
         rootPublic.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0 = root.createChildNode(0);
      HdKeyNode m_0Public = m_0.getPublicNode();
      extPub = m_0Public.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_PUB, extPub);
      try {
         m_0Public.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0_2147483647H = m_0.createChildNode(2147483647 | HARDENED_PRIVATE_KEY);
      HdKeyNode m_0_2147483647HPublic = m_0_2147483647H.getPublicNode();
      extPub = m_0_2147483647HPublic.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_PUB, extPub);
      try {
         m_0_2147483647HPublic.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0_2147483647H_1 = m_0_2147483647H.createChildNode(1);
      HdKeyNode m_0_2147483647H_1Public = m_0_2147483647H_1.getPublicNode();
      extPub = m_0_2147483647H_1Public.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_PUB, extPub);
      try {
         m_0_2147483647H_1Public.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      HdKeyNode m_0_2147483647H_1_2147483646H = m_0_2147483647H_1.createChildNode(2147483646 | HARDENED_PRIVATE_KEY);
      HdKeyNode m_0_2147483647H_1_2147483646HPublic = m_0_2147483647H_1_2147483646H.getPublicNode();
      extPub = m_0_2147483647H_1_2147483646HPublic.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_2147483646H_PUB, extPub);
      try {
         m_0_2147483647H_1_2147483646HPublic.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }

      // This time create from parent public node as we are no longer depending
      // on hardened nodes
      HdKeyNode m_0_2147483647H_1_2147483646H_2Public = m_0_2147483647H_1_2147483646HPublic.createChildNode(2);
      extPub = m_0_2147483647H_1_2147483646H_2Public.getPublicNode().serialize(NetworkParameters.productionNetwork);
      Assert.assertEquals(TV2_TEST_M_0_2147483647H_1_2147483646H_2_PUB, extPub);
      try {
         m_0_2147483647H_1_2147483646H_2Public.getPrivateKey();
         Assert.fail("Exception expected");
      } catch (KeyGenerationException e) {
         // Expected
      }
   }

}
