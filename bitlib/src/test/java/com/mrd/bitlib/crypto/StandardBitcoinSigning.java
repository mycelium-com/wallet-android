package com.mrd.bitlib.crypto;

import com.mrd.bitlib.crypto.ec.EcTools;
import com.mrd.bitlib.crypto.ec.Point;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StandardBitcoinSigning {
   private static InMemoryPrivateKey privKey;
   private static Address address;

   @BeforeClass
   public static void createFixtures() {
      address = Address.fromString("16F9yVJYb267n8rq5sDCU2xmDpxNajUfLV");
      NetworkParameters network = NetworkParameters.productionNetwork;
      privKey = new InMemoryPrivateKey("KxrnQTQKTZv2y75BUUVFPrFFjWyjaEy8hqYXEkvdMMfgrGoo6XHB", network);
      Assert.assertEquals(network, address.getNetwork());
      Assert.assertEquals(privKey.getPublicKey().toAddress(network, AddressType.P2PKH), address);
   }

   @Test
   public void testVerification() throws WrongSignatureException {
      String message = "Hello, this is Mycelium";
      String signature = "H9MwYMb/ctDd6BcFvtQKUjwq990y3xSm2K6WFZpMx5+7e+G5Ffqm/imFig0VKtiPL1GDryArVJcoEemLAN4+Z9Q=";
      SignedMessage signedMessage = SignedMessage.validate(address, message, signature);
      PublicKey pubkey = signedMessage.getPublicKey();
      Assert.assertEquals(address, pubkey.toAddress(address.getNetwork(), AddressType.P2PKH));
   }

   @Test
   public void testFailsWrongSignature() {
      String message = "Hello, this is NOT Mycelium";
      String signature = "H9MwYMb/ctDd6BcFvtQKUjwq990y3xSm2K6WFZpMx5+7e+G5Ffqm/imFig0VKtiPL1GDryArVJcoEemLAN4+Z9Q=";
      assertFailingSig(message, signature);
      message = "Hello, this is Mycelium";
      signature = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=";
      assertFailingSig(message, signature);
   }

   private void assertFailingSig(String message, String signature) {
      try {
         SignedMessage.validate(address, message, signature);
         fail("signature was valid, this is not expected");
      } catch (WrongSignatureException e) {
         System.out.println(e.getMessage());
      }
   }

   @Test
   public void testSignatureCreation() throws WrongSignatureException {
      byte[] abcMessage = new byte[]{24, 66, 105, 116, 99, 111, 105, 110, 32, 83, 105, 103, 110, 101, 100, 32, 77, 101, 115, 115, 97, 103, 101, 58, 10, 3, 97, 98, 99};
      String message = "abc";
      byte[] msg = Signatures.formatMessageForSigning(message);
      Assert.assertArrayEquals(abcMessage, msg);
      SignedMessage signed2 = privKey.signMessage(message);
      assertEquals(privKey.getPublicKey(), signed2.getPublicKey());
      String sigStr  = signed2.getBase64Signature();
      System.out.println(sigStr);
      PublicKey recoverFromSignature = SignedMessage.recoverFromSignature(message, sigStr);
      assertEquals(privKey.getPublicKey(), recoverFromSignature);
   }

   @Test
   public void testKeyDecompress() {
      Point point = EcTools.decompressKey(BigInteger.ONE, true);
      BigInteger expectedY = new BigInteger("85895366384747149408010284714111852077055649506395260922968891100383188440129");
      assertEquals(expectedY, point.getY().toBigInteger());
   }
}
