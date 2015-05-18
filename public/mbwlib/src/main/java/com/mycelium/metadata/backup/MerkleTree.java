package com.mycelium.metadata.backup;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.util.ArrayList;
import java.util.List;

// implements https://github.com/oleganza/bitcoin-papers/blob/master/AutomaticEncryptedWalletBackups.md#definitions
public class MerkleTree{
   public static final int DEFAULT_CHUNK_SIZE = 1024;
   private Sha256Hash root;
   private ArrayList<byte[]> chunks;


   public MerkleTree(Sha256Hash root, ArrayList<byte[]> chunks) {
      this.root = root;
      this.chunks = chunks;
   }

   public static MerkleTree fromData(byte[] data){
      return fromData(data, DEFAULT_CHUNK_SIZE);
   }

   public static MerkleTree fromData(byte[] data, int chunkSize){
      ArrayList<byte[]> chunks = new ArrayList<byte[]>( (int)Math.ceil( (double) data.length / chunkSize) );
      ByteReader reader = new ByteReader(data);
      while(true){
         int available = reader.available();
         boolean isFin = (available <= chunkSize);

         byte[] chunk;
         try {
            chunk = reader.getBytes( Math.min(available, chunkSize) );
         } catch (ByteReader.InsufficientBytesException e) {
            throw new RuntimeException(e);
         }

         chunks.add(chunk);

         // we got the last chunk - bail
         if (isFin){
            break;
         }
      }

      Sha256Hash root = calcRoot(chunks);

      return new MerkleTree(root, chunks);

   }

   private static Sha256Hash calcRoot(ArrayList<byte[]> chunks){
      ArrayList<Sha256Hash> hashList = new ArrayList<Sha256Hash>(Lists.transform(chunks, new Function<byte[], Sha256Hash>() {
         @Override
         public Sha256Hash apply(byte[] input) {
            return HashUtils.doubleSha256(input);
         }
      }));

      return calcParentLayer(hashList).get(0);
   }

   private static ArrayList<Sha256Hash> calcParentLayer(final List<Sha256Hash> chunksHashes){
      if (chunksHashes.size() == 1){
         return Lists.newArrayList(chunksHashes.get(0));
      }

      // if the current layer has an odd number of elements, append the last element as padding
      if (chunksHashes.size() % 2 != 0){
         chunksHashes.add(Iterables.getLast(chunksHashes));
      }

      ArrayList<Sha256Hash> ret = new ArrayList<Sha256Hash>((int) Math.floor( (double)chunksHashes.size() / 2 ));
      for (int pos = 0; pos < chunksHashes.size(); pos+=2){
         ret.add(HashUtils.doubleSha256TwoBuffers(
               chunksHashes.get(pos).getBytes(),
               chunksHashes.get(pos + 1).getBytes()
         ));
      }

      return calcParentLayer(ret);
   }

   public Sha256Hash getRoot() {
      return root;
   }

   public ArrayList<byte[]> getChunks() {
      return chunks;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MerkleTree that = (MerkleTree) o;

      if (!root.equals(that.root)) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return root.hashCode();
   }
}
