package com.mrd.bitlib.model.hdpath;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.primitives.UnsignedInteger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HdKeyPath implements Serializable {

   public static final String HARDENED_MARKER = "'";
   private final HdKeyPath parent;

   protected final UnsignedInteger index;
   private final boolean hardened;

   public static final HdKeyPath ROOT = new HdKeyPath();
   public static final Bip44Purpose BIP44 = ROOT.getBip44Purpose();
   public static final HdKeyPath BIP32_ROOT = ROOT.getHardenedChild(0);
   public static final Bip44CoinType BIP44_TESTNET = BIP44.getCoinTypeBitcoinTestnet();
   public static final Bip44CoinType BIP44_PRODNET = BIP44.getCoinTypeBitcoin();


   public static HdKeyPath valueOf(String path) {
      Iterator<String> iterator = Splitter.on("/").split(path).iterator();
      Preconditions.checkState("m".equals(iterator.next()));
      return ROOT.getChild(iterator);
   }


   public HdKeyPath getChild(String path){
      Iterator<String> iterator = Splitter.on("/").split(path).iterator();
      return this.getChild(iterator);
   }


   private HdKeyPath getChild(Iterator<String> path){
      if (!path.hasNext()) return this;

      String ak = path.next();
      int index = Integer.valueOf(ak.replace(HARDENED_MARKER, ""));

      if (ak.endsWith(HARDENED_MARKER)){
         return this.getHardenedChild(index).getChild(path);
      } else {
         return this.getNonHardenedChild(index).getChild(path);
      }
   }

   @SuppressWarnings("unchecked")
   public <T extends HdKeyPath> Optional<T> findPartOf(Class<T> ofClass){
      HdKeyPath ak = this;
      while (ak.parent != null && !ak.getClass().equals(ofClass)){
         ak = ak.parent;
      }

      if (ak.getClass().equals(ofClass)) {
         return Optional.of((T)ak);
      }else{
         return Optional.absent();
      }
   }

   public HdKeyPath(HdKeyPath parent, UnsignedInteger index,boolean hardened) {
      this.parent = parent;
      this.hardened = hardened;
      this.index = index;
   }

   private HdKeyPath() {
      this.parent = null;
      this.index = UnsignedInteger.ZERO;
      hardened = true;
   }

   public boolean isHardened(){
       return hardened;
   }

   private HdKeyPath getChild(int index) {
      boolean hardened = index < 0;
      int value = index & Integer.MAX_VALUE;
      return hardened ? getHardenedChild(value): getNonHardenedChild(value);
   }

   public HdKeyPath getNonHardenedChild(int index) {
      Preconditions.checkArgument(index >= 0);
      return knownChildFactory(UnsignedInteger.valueOf(index), false);
   }


   public HdKeyPath getHardenedChild(int index){
      Preconditions.checkArgument(index >= 0);
      //Preconditions.checkState(this.parent.isHardened());  --> maybe in bip44
      return knownChildFactory(UnsignedInteger.valueOf(index), true);
   }

   protected HdKeyPath knownChildFactory(UnsignedInteger index, boolean hardened){
      if (index.equals(UnsignedInteger.valueOf(44)) && hardened){
         return new Bip44Purpose(this, index, true);
      }else{
         return new HdKeyPath(this, index, hardened);
      }
   }

   public Bip44Purpose getBip44Purpose(){
      return new Bip44Purpose(this, UnsignedInteger.valueOf(44), true);
   }


   private int getValue(){
      return index.intValue() | (isHardened() ? 1<<31 : 0);
   }

   public List<Integer> getAddressN(){
      ArrayList<Integer> ret = new ArrayList<Integer>(10);
      HdKeyPath ak = this;
      while (ak.parent != null){
         ret.add(0, ak.getValue());
         ak = ak.parent;
      }

      return ret;
   }

   @Override
   public String toString() {
      if (parent == null) return "m";
      return parent.toString()+"/"+index+(isHardened()? HARDENED_MARKER :"");
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof HdKeyPath)) return false;

      HdKeyPath hdKeyPath = (HdKeyPath) o;

      if (hardened != hdKeyPath.hardened) return false;
      if (!index.equals(hdKeyPath.index)) return false;
      if (!parent.equals(hdKeyPath.parent)) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = parent.hashCode();
      result = 31 * result + index.hashCode();
      result = 31 * result + (hardened ? 1 : 0);
      return result;
   }

   // returns the raw index (including the hardened bit, if set) of the last path element
   public int getLastRawIndex() {
      return getValue();
   }

   // returns the index of the last path element
   public int getLastIndex() {
      return index.intValue();
   }
}
