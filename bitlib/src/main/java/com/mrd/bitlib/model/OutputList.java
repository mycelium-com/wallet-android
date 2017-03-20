package com.mrd.bitlib.model;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.TransactionUtils;

import java.util.ArrayList;

public class OutputList extends ArrayList<TransactionOutput> {

   public boolean add(long value, ScriptOutput script){
      Preconditions.checkArgument(value >= 0);
      return this.add(new TransactionOutput(value, script));
   }

   public long getTotalAmount(){
      long sum = 0;
      for (TransactionOutput out : this){
         sum += out.value;
      }

      return sum;
   }

   public OutputList newOutputsWithTotalAmount(Long amountToSend) {
      Preconditions.checkState(getTotalAmount() == 0);
      Preconditions.checkState(size() > 0);
      Preconditions.checkArgument(amountToSend >= TransactionUtils.MINIMUM_OUTPUT_VALUE);
      OutputList ret = new OutputList();
      // distribute the amount over all outputs, fill up all first n-1 of n outputs with
      // floor(amountToSend / n) and the n'th one with the remaining amount to circumvent
      // rounding errors

      long split = (long)Math.floor((double)amountToSend / size());

      // ensure that its the min-spendable amount
      if (split < TransactionUtils.MINIMUM_OUTPUT_VALUE){
         // dump everything into first output
         ret.add(new TransactionOutput(amountToSend, get(0).script));
      } else {
         long spentAmount = 0;
         for (int idx = 0; idx < size() - 1; idx++) {
            ret.add(new TransactionOutput(split, get(idx).script));
            spentAmount += split;
         }
         ret.add(new TransactionOutput(amountToSend - spentAmount, get(size()-1).script));
      }

      Preconditions.checkState(ret.getTotalAmount() == amountToSend);

      return ret;
   }
}
