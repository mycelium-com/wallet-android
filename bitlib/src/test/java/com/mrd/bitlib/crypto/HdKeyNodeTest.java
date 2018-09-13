package com.mrd.bitlib.crypto;

import com.mrd.bitlib.crypto.HdKeyNode.KeyGenerationException;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.HexUtils;

import org.junit.Test;

import static com.mrd.bitlib.model.NetworkParameters.productionNetwork;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HdKeyNodeTest {
    // test vectors from https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
    private static final byte[] TV1_SEED = HexUtils.toBytes("000102030405060708090a0b0c0d0e0f");
    private static final byte[] TV2_SEED = HexUtils.toBytes("fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542");
    private static final byte[] TV3_SEED = HexUtils.toBytes("4b381541583be4423346c643850da4b320e46a87ae3d2a4e6da11eba819cd4acba45d239319ac14f863b8d5ab5a0d0c64d2e8a1e7d1457df2e5a3c51c73235be");

    private static final TestVector[] TEST_VECTORS = new TestVector[]{
            new TestVector(TV1_SEED, "m",
                    "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8",
                    "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi"),
            new TestVector(TV1_SEED, "m/0H",
                    "xpub68Gmy5EdvgibQVfPdqkBBCHxA5htiqg55crXYuXoQRKfDBFA1WEjWgP6LHhwBZeNK1VTsfTFUHCdrfp1bgwQ9xv5ski8PX9rL2dZXvgGDnw",
                    "xprv9uHRZZhk6KAJC1avXpDAp4MDc3sQKNxDiPvvkX8Br5ngLNv1TxvUxt4cV1rGL5hj6KCesnDYUhd7oWgT11eZG7XnxHrnYeSvkzY7d2bhkJ7"),
            new TestVector(TV1_SEED, "m/0H/1",
                    "xpub6ASuArnXKPbfEwhqN6e3mwBcDTgzisQN1wXN9BJcM47sSikHjJf3UFHKkNAWbWMiGj7Wf5uMash7SyYq527Hqck2AxYysAA7xmALppuCkwQ",
                    "xprv9wTYmMFdV23N2TdNG573QoEsfRrWKQgWeibmLntzniatZvR9BmLnvSxqu53Kw1UmYPxLgboyZQaXwTCg8MSY3H2EU4pWcQDnRnrVA1xe8fs"),
            new TestVector(TV1_SEED, "m/0H/1/2H",
                    "xpub6D4BDPcP2GT577Vvch3R8wDkScZWzQzMMUm3PWbmWvVJrZwQY4VUNgqFJPMM3No2dFDFGTsxxpG5uJh7n7epu4trkrX7x7DogT5Uv6fcLW5",
                    "xprv9z4pot5VBttmtdRTWfWQmoH1taj2axGVzFqSb8C9xaxKymcFzXBDptWmT7FwuEzG3ryjH4ktypQSAewRiNMjANTtpgP4mLTj34bhnZX7UiM"),
            new TestVector(TV1_SEED, "m/0H/1/2H/2",
                    "xpub6FHa3pjLCk84BayeJxFW2SP4XRrFd1JYnxeLeU8EqN3vDfZmbqBqaGJAyiLjTAwm6ZLRQUMv1ZACTj37sR62cfN7fe5JnJ7dh8zL4fiyLHV",
                    "xprvA2JDeKCSNNZky6uBCviVfJSKyQ1mDYahRjijr5idH2WwLsEd4Hsb2Tyh8RfQMuPh7f7RtyzTtdrbdqqsunu5Mm3wDvUAKRHSC34sJ7in334"),
            new TestVector(TV1_SEED, "m/0H/1/2H/2/1000000000",
                    "xpub6H1LXWLaKsWFhvm6RVpEL9P4KfRZSW7abD2ttkWP3SSQvnyA8FSVqNTEcYFgJS2UaFcxupHiYkro49S8yGasTvXEYBVPamhGW6cFJodrTHy",
                    "xprvA41z7zogVVwxVSgdKUHDy1SKmdb533PjDz7J6N6mV6uS3ze1ai8FHa8kmHScGpWmj4WggLyQjgPie1rFSruoUihUZREPSL39UNdE3BBDu76"),
            new TestVector(TV2_SEED, "m",
                    "xpub661MyMwAqRbcFW31YEwpkMuc5THy2PSt5bDMsktWQcFF8syAmRUapSCGu8ED9W6oDMSgv6Zz8idoc4a6mr8BDzTJY47LJhkJ8UB7WEGuduB",
                    "xprv9s21ZrQH143K31xYSDQpPDxsXRTUcvj2iNHm5NUtrGiGG5e2DtALGdso3pGz6ssrdK4PFmM8NSpSBHNqPqm55Qn3LqFtT2emdEXVYsCzC2U"),
            new TestVector(TV2_SEED, "m/0",
                    "xpub69H7F5d8KSRgmmdJg2KhpAK8SR3DjMwAdkxj3ZuxV27CprR9LgpeyGmXUbC6wb7ERfvrnKZjXoUmmDznezpbZb7ap6r1D3tgFxHmwMkQTPH",
                    "xprv9vHkqa6EV4sPZHYqZznhT2NPtPCjKuDKGY38FBWLvgaDx45zo9WQRUT3dKYnjwih2yJD9mkrocEZXo1ex8G81dwSM1fwqWpWkeS3v86pgKt"),
            new TestVector(TV2_SEED, "m/0/2147483647H",
                    "xpub6ASAVgeehLbnwdqV6UKMHVzgqAG8Gr6riv3Fxxpj8ksbH9ebxaEyBLZ85ySDhKiLDBrQSARLq1uNRts8RuJiHjaDMBU4Zn9h8LZNnBC5y4a",
                    "xprv9wSp6B7kry3Vj9m1zSnLvN3xH8RdsPP1Mh7fAaR7aRLcQMKTR2vidYEeEg2mUCTAwCd6vnxVrcjfy2kRgVsFawNzmjuHc2YmYRmagcEPdU9"),
            new TestVector(TV2_SEED, "m/0/2147483647H/1",
                    "xpub6DF8uhdarytz3FWdA8TvFSvvAh8dP3283MY7p2V4SeE2wyWmG5mg5EwVvmdMVCQcoNJxGoWaU9DCWh89LojfZ537wTfunKau47EL2dhHKon",
                    "xprv9zFnWC6h2cLgpmSA46vutJzBcfJ8yaJGg8cX1e5StJh45BBciYTRXSd25UEPVuesF9yog62tGAQtHjXajPPdbRCHuWS6T8XA2ECKADdw4Ef"),
            new TestVector(TV2_SEED, "m/0/2147483647H/1/2147483646H",
                    "xpub6ERApfZwUNrhLCkDtcHTcxd75RbzS1ed54G1LkBUHQVHQKqhMkhgbmJbZRkrgZw4koxb5JaHWkY4ALHY2grBGRjaDMzQLcgJvLJuZZvRcEL",
                    "xprvA1RpRA33e1JQ7ifknakTFpgNXPmW2YvmhqLQYMmrj4xJXXWYpDPS3xz7iAxn8L39njGVyuoseXzU6rcxFLJ8HFsTjSyQbLYnMpCqE2VbFWc"),
            new TestVector(TV2_SEED, "m/0/2147483647H/1/2147483646H/2",
                    "xpub6FnCn6nSzZAw5Tw7cgR9bi15UV96gLZhjDstkXXxvCLsUXBGXPdSnLFbdpq8p9HmGsApME5hQTZ3emM2rnY5agb9rXpVGyy3bdW6EEgAtqt",
                    "xprvA2nrNbFZABcdryreWet9Ea4LvTJcGsqrMzxHx98MMrotbir7yrKCEXw7nadnHM8Dq38EGfSh6dqA9QWTyefMLEcBYJUuekgW4BYPJcr9E7j"),
            new TestVector(TV3_SEED, "m",
                    "xpub661MyMwAqRbcEZVB4dScxMAdx6d4nFc9nvyvH3v4gJL378CSRZiYmhRoP7mBy6gSPSCYk6SzXPTf3ND1cZAceL7SfJ1Z3GC8vBgp2epUt13",
                    "xprv9s21ZrQH143K25QhxbucbDDuQ4naNntJRi4KUfWT7xo4EKsHt2QJDu7KXp1A3u7Bi1j8ph3EGsZ9Xvz9dGuVrtHHs7pXeTzjuxBrCmmhgC6"),
            new TestVector(TV3_SEED, "m/0H",
                    "xpub68NZiKmJWnxxS6aaHmn81bvJeTESw724CRDs6HbuccFQN9Ku14VQrADWgqbhhTHBaohPX4CjNLf9fq9MYo6oDaPPLPxSb7gwQN3ih19Zm4Y",
                    "xprv9uPDJpEQgRQfDcW7BkF7eTya6RPxXeJCqCJGHuCJ4GiRVLzkTXBAJMu2qaMWPrS7AANYqdq6vcBcBUdJCVVFceUvJFjaPdGZ2y9WACViL4L")
    };

    @Test
    public void reproduceVectorsTest() {
        for (TestVector tv : TEST_VECTORS) {
            HdKeyNode childNode = HdKeyNode.fromSeed(tv.seed, null).createChildNode(tv.derivation);
            assertEquals(tv.xpriv, childNode.serialize(productionNetwork, BipDerivationType.BIP44));
            assertEquals(tv.xpub, childNode.getPublicNode().serialize(productionNetwork, BipDerivationType.BIP44));
        }
    }

    @Test
    public void failOnHardenedChildOfXpubTest() {
        HdKeyNode rootXpub = HdKeyNode.fromSeed(TEST_VECTORS[0].seed, null).getPublicNode();
        String[] paths = {"m/0'", "m/3/4/5'/6"};
        for (String path : paths) {
            try {
                rootXpub.createChildNode(HdKeyPath.valueOf(path));
                fail("It is not possible to get hardened nodes from xpub");
            } catch (KeyGenerationException e) {
                // Expected
            }
        }
    }

    @Test
    public void failOnPrivChildFromXpubTest() {
        HdKeyNode rootXpub = HdKeyNode.fromSeed(TEST_VECTORS[0].seed, null).getPublicNode();
        String[] paths = {"m", "m/0", "m/3/4/5/6"};
        for (String path : paths) {
            try {
                rootXpub.createChildNode(HdKeyPath.valueOf(path)).getPrivateKey();
                fail("It is not possible to get private keys from xpub");
            } catch (KeyGenerationException e) {
                // Expected
            }
        }
    }

    @Test
    public void createChildNodeIntTest() {
        TestVector tv = TEST_VECTORS[3];
        assertEquals("m/0'/1/2'", tv.derivation.toString());
        HdKeyNode node = HdKeyNode.fromSeed(tv.seed, null).createHardenedChildNode(0).createChildNode(1).createHardenedChildNode(2);
        assertEquals(tv.xpriv, node.serialize(productionNetwork, BipDerivationType.BIP44));
    }

    private static class TestVector {
        byte[] seed;
        HdKeyPath derivation;
        String xpub;
        String xpriv;

        TestVector(byte[] seed, String derivation, String xpub, String xpriv) {
            this.seed = seed;
            this.derivation = HdKeyPath.valueOf(derivation.replace("H", "'"));
            this.xpub = xpub;
            this.xpriv = xpriv;
        }
    }
}
