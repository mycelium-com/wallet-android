package com.mycelium.metadata.backup;

import com.mrd.bitlib.crypto.PrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.util.Arrays;


public class BackupPayload {
   private final byte version;
   private final int timestamp;
   private final byte[] iv;
   private final byte[] cipherText;
   private final byte[] signature;
   private final MerkleTree merkleTree;


   public static BackupPayload deserialize(byte[] data){
      ByteReader reader = new ByteReader(data);

      try {
         byte version = reader.get();
         int timestamp = reader.getIntLE();

         byte[] iv = reader.getBytes(16);

         int cipherTextLen = (int)reader.getCompactInt();
         byte[] cipherText = reader.getBytes(cipherTextLen);

         int sigLen = (int)reader.getCompactInt();
         byte[] signature = reader.getBytes(sigLen);

         return new BackupPayload(version, timestamp, iv, cipherText, signature);

      } catch (ByteReader.InsufficientBytesException e) {
         throw new IllegalArgumentException("Backup payload invalid");
      }
   }

   public byte[] serialize(){
      ByteWriter writer = new ByteWriter(1024);
      writer.put(version);
      writer.putIntLE(timestamp);
      writer.putBytes(iv);
      writer.putCompactInt(cipherText.length);
      writer.putBytes(cipherText);
      writer.putCompactInt(signature.length);
      writer.putBytes(signature);
      return writer.toBytes();
   }

   private BackupPayload(byte version, int timestamp, byte[] iv, byte[] cipherText, byte[] signature) {
      this.version = version;
      this.timestamp = timestamp;
      this.iv = iv;
      this.cipherText = cipherText;
      this.signature = signature;
      this.merkleTree = MerkleTree.fromData(cipherText);
   }

   public BackupPayload(byte version, int timestamp, byte[] iv, byte[] cipherText, PrivateKey signatureKey) {
      this.version = version;
      this.timestamp = timestamp;
      this.iv = iv;
      this.cipherText = cipherText;
      this.merkleTree = MerkleTree.fromData(cipherText);
      this.signature = calcSignature(signatureKey);
   }

   private byte[] calcSignature(PrivateKey signatureKey) {
      SignedMessage signedMessage = signatureKey.signHash(getHashedContentToSign());
      return signedMessage.getDerEncodedSignature();
   }


   // used for checking the signature
   public Sha256Hash getHashedContentToSign(){
      ByteWriter writer = new ByteWriter(1 + 4 + 16 + 32);
      writer.put(version);
      writer.putIntLE(timestamp);
      writer.putBytes(iv);
      writer.putBytes(merkleTree.getRoot().getBytes());
      return HashUtils.doubleSha256(writer.toBytes());
   }

   public boolean verifySignature(final byte[] apub) {
      com.mrd.bitlib.crypto.PublicKey publicKey = new com.mrd.bitlib.crypto.PublicKey(apub);
      Sha256Hash hashedContent = getHashedContentToSign();
      return publicKey.verifyDerEncodedSignature(hashedContent, signature);
   }

   public byte[] getSignature() {
      return signature;
   }


   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      BackupPayload that = (BackupPayload) o;

      if (timestamp != that.timestamp) return false;
      if (version != that.version) return false;
      if (!Arrays.equals(cipherText, that.cipherText)) return false;
      if (!Arrays.equals(iv, that.iv)) return false;
      if (!merkleTree.equals(that.merkleTree)) return false;
      if (!Arrays.equals(signature, that.signature)) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = (int) version;
      result = 31 * result + timestamp;
      result = 31 * result + Arrays.hashCode(iv);
      result = 31 * result + Arrays.hashCode(cipherText);
      result = 31 * result + Arrays.hashCode(signature);
      result = 31 * result + merkleTree.hashCode();
      return result;
   }
}

