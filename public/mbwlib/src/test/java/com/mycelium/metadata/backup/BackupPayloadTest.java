package com.mycelium.metadata.backup;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PrivateKey;
import com.mrd.bitlib.util.HexUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class BackupPayloadTest {

   @Test
   public void testNewPayload(){
      byte[] cipherText = HexUtils.toBytes("5edbaade9ba4ed528a8de36c95ece996189dedf4756fba2599f94b4f370d7013" +
            "66e2f0ba4e59111c0787708cf4b0b82de558b4d8bf5d90b3512f09814d605d4c" +
            "14f2f85b596211f83918c31c4bef19ea");

      byte[] sig = HexUtils.toBytes("3045022100ddbc9b06625c2b3c9cbfb27b6ac39596bd13daf43d4ddecbb7257a" +
            "0d26f5e2c402200a5bd5fd27df7ac262ac3cff9d5398742c6fd9c76c42754866" +
            "7bee45dcb1134c");

      byte[] iv = HexUtils.toBytes("bf07aaa979ae8af6eebfea5da8e83cad");
      PrivateKey sigKey = new InMemoryPrivateKey(HexUtils.toBytes("44b45878c33c974179f5363fee95f9e9d4a60c97e9c865e58b57bef3558034f4"), true);
      int timestamp = 1427720967;



      assertEquals("Public Key",
            "028747be6de07552c48f9db23617792d47df1accd611175f6dfe636f4098984a09",
            HexUtils.toHex(sigKey.getPublicKey().getPublicKeyBytes())
            );

      BackupPayload payload = new BackupPayload((byte)1, timestamp, iv, cipherText,  sigKey);

      assertEquals("Content hash",
            "89bfc7bf81f8e3b2944c4903b68034dc7317806b40f2d225fe4b13907fdb225d",
            HexUtils.toHex(payload.getHashedContentToSign().getBytes())
      );

      assertTrue("Signature verifies", payload.verifySignature( sigKey.getPublicKey().getPublicKeyBytes() ) );

      /* check if we are deterministic */
      assertEquals("Deterministic signature check",
            HexUtils.toHex(sig),
            HexUtils.toHex(payload.getSignature())
            );

   }

   @Test
   public void testSerialization(){
      byte[] encryptedContent = HexUtils.toBytes("01074b1955bf07aaa979ae8af6eebfea5da8e83cad505edbaade9ba4ed528a8d" +
            "e36c95ece996189dedf4756fba2599f94b4f370d701366e2f0ba4e59111c0787" +
            "708cf4b0b82de558b4d8bf5d90b3512f09814d605d4c14f2f85b596211f83918" +
            "c31c4bef19ea473045022100ddbc9b06625c2b3c9cbfb27b6ac39596bd13daf4" +
            "3d4ddecbb7257a0d26f5e2c402200a5bd5fd27df7ac262ac3cff9d5398742c6f" +
            "d9c76c427548667bee45dcb1134c");

      BackupPayload payload = BackupPayload.deserialize(encryptedContent);
      byte[] serialized = payload.serialize();

      assertEquals("Byte representation",
            HexUtils.toHex(encryptedContent),
            HexUtils.toHex(serialized)
      );

      BackupPayload payload2 = BackupPayload.deserialize(serialized);
      assertTrue("Equals operator", payload.equals(payload2));
      assertEquals("Content to hash",
            HexUtils.toHex(payload.getHashedContentToSign().getBytes()),
            HexUtils.toHex(payload2.getHashedContentToSign().getBytes())
      );
   }

   @Test
   public void testDeserialize() throws Exception {
      // test vectors from https://github.com/oleganza/bitcoin-papers/blob/master/AutomaticEncryptedWalletBackups.md#test-vectors

      byte[] encryptedContent = HexUtils.toBytes("01074b1955bf07aaa979ae8af6eebfea5da8e83cad505edbaade9ba4ed528a8d" +
            "e36c95ece996189dedf4756fba2599f94b4f370d701366e2f0ba4e59111c0787" +
            "708cf4b0b82de558b4d8bf5d90b3512f09814d605d4c14f2f85b596211f83918" +
            "c31c4bef19ea473045022100ddbc9b06625c2b3c9cbfb27b6ac39596bd13daf4" +
            "3d4ddecbb7257a0d26f5e2c402200a5bd5fd27df7ac262ac3cff9d5398742c6f" +
            "d9c76c427548667bee45dcb1134c");

      byte[] apub = HexUtils.toBytes("028747be6de07552c48f9db23617792d47df1accd611175f6dfe636f4098984a09");
      byte[] wrongApub = HexUtils.toBytes("028747be6de07552c48f9db23617792d47df1accd611175f6dfe636f4098984a08");

      BackupPayload payload = BackupPayload.deserialize(encryptedContent);

      assertEquals("content hash",
            "89bfc7bf81f8e3b2944c4903b68034dc7317806b40f2d225fe4b13907fdb225d",
            HexUtils.toHex(payload.getHashedContentToSign().getBytes()));

      assertTrue("signature check",
            payload.verifySignature(apub));

      assertFalse("failing signature check",
            payload.verifySignature(wrongApub));
   }
}