package com.mrd.bitlib.model.hdpath;


import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.primitives.UnsignedInteger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HdKeyPath implements Serializable {
   private static final String HARDENED_MARKER = "'";
   public static final HdKeyPath ROOT = new HdKeyPath();
   public static final HdKeyPath BIP44 = ROOT.getHardenedChild(44);
   public static final HdKeyPath BIP49 = ROOT.getHardenedChild(49);
   public static final HdKeyPath BIP84 = ROOT.getHardenedChild(84);
   public static final HdKeyPath BIP32_ROOT = ROOT.getHardenedChild(0);

   private final HdKeyPath parent;
   private final UnsignedInteger index;
   private final boolean hardened;

   public static HdKeyPath valueOf(String path) {
      Iterator<String> iterator = Splitter.on("/").split(path).iterator();
      Preconditions.checkState("m".equals(iterator.next()));
      return ROOT.getChild(iterator);
   }

   public HdKeyPath getParent() {
      return parent;
   }

   private HdKeyPath getChild(Iterator<String> path){
      if (!path.hasNext()) {
         return this;
      }

      String ak = path.next();
      int index = Integer.valueOf(ak.replace(HARDENED_MARKER, ""));

      if (ak.endsWith(HARDENED_MARKER)){
         return getHardenedChild(index).getChild(path);
      } else {
         return getNonHardenedChild(index).getChild(path);
      }
   }

   public HdKeyPath(HdKeyPath parent, UnsignedInteger index, boolean hardened) {
      this.parent = parent;
      this.hardened = hardened;
      this.index = index;
   }

   private HdKeyPath() {
      this(null, UnsignedInteger.ZERO, true);
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
      return new HdKeyPath(this, UnsignedInteger.valueOf(index), false);
   }

   public HdKeyPath getHardenedChild(int index){
      Preconditions.checkArgument(index >= 0);
      return new HdKeyPath(this, UnsignedInteger.valueOf(index), true);
   }

   private int getValue() {
      return index.intValue() | (isHardened() ? 1<<31 : 0); // 0x80000000 ?= 1<<31
   }

   public List<Integer> getAddressN(){
      ArrayList<Integer> ret = new ArrayList<>(10);
      HdKeyPath ak = this;
      while (ak.parent != null){
         ret.add(0, ak.getValue());
         ak = ak.parent;
      }
      return ret;
   }

   @Override
   public String toString() {
      if (parent == null) {
         return "m";
      }
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

   // returns the index of the last path element
   public int getLastIndex() {
      return index.intValue();
   }

   // some helper methods to get hardening and ordering right. mainnet is 0, testnet is 1, external is 0, change chain is 1, ...
   public HdKeyPath getCoinTypeBitcoin(boolean isTestnet) {
      return getHardenedChild(isTestnet ? 1 : 0);
   }

   public HdKeyPath getAccount(int accountIndex) {
      return getHardenedChild(accountIndex);
   }

   public HdKeyPath getChain(boolean isExternal) {
      return getNonHardenedChild(isExternal ? 0 : 1);
   }

   public HdKeyPath getAddress(int index) {
      return getNonHardenedChild(index);
   }
}
