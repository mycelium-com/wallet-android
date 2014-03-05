package com.mycelium.lt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.crypto.WrongSignatureException;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

public class ApiUtils {

   private static final String SIGNATURE_PREFIX = "Mycelium Local Trader:";

   public static boolean validateUuidHashSignature(Address address, UUID uuid, String signatureBase64) {
      if (address == null || uuid == null || signatureBase64 == null) {
         return false;
      }
      String message = uuidToMessage(uuid);
      try {
         SignedMessage.validate(address, message, signatureBase64);
      } catch (WrongSignatureException e) {
         return false;
      }
      return true;
   }

   private static String uuidToMessage(UUID uuid){
      byte[] uuidBytes = uuidToBytes(uuid);
      Sha256Hash uuidHash = HashUtils.doubleSha256(uuidBytes);
      return new StringBuilder().append(SIGNATURE_PREFIX).append(uuidHash.toHex()).toString();
   }
   
   public static String generateUuidHashSignature(InMemoryPrivateKey key, UUID uuid, RandomSource randomSource) {
      return key.signMessage(uuidToMessage(uuid), randomSource).getBase64Signature();
   }

   protected static byte[] uuidToBytes(UUID uuid) {
      ByteArrayOutputStream ba = new ByteArrayOutputStream(16);
      DataOutputStream da = new DataOutputStream(ba);
      try {
         da.writeLong(uuid.getMostSignificantBits());
         da.writeLong(uuid.getLeastSignificantBits());
      } catch (IOException e) {
         // Never happens
      }
      return ba.toByteArray();
   }

}
