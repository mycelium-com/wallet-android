package com.mrd.bitlib.model;

public class ConfirmationRiskProfile {
   public static ConfirmationRiskProfile MINED = new ConfirmationRiskProfile(-1, false);

   /**
    *  shows if this transaction references other unconfirmed transactions
    *    if this transaction is already mined -> -1
    *    if all referenced inputs are confirmed -> 0
    *    if one or more input is unconfirmed -> max(unconfirmedChainLength of all inputs) + 1
    *    -> i.e. there are at least `unconfirmedChainLength`-transactions that need to get mined until this tx can get mined.
    *
    *    @param txHash the hash of the transaction
    *
    *    @return
    *       -1 if tx is already mined
    *       0 if all inputs are mined but tx is still unconfirmed
    *       > 0 if one of its funding inputs are also unconfirmed
    *
    */
   public final int unconfirmedChainLength;

   /**
    * true if this transaction or any unconfirmed tx it relies on is marked for RBF
    */
   public final boolean hasRbfRisk;

   public ConfirmationRiskProfile(int unconfirmedChainLength, boolean hasRbfRisk) {
      this.unconfirmedChainLength = unconfirmedChainLength;
      this.hasRbfRisk = hasRbfRisk;
   }

   @Override
   public String toString() {
      if (this.equals(MINED)) {
         return "mined";
      } else {
         return String.format("unconfirmedChainLength: %d rbfAble: %s", unconfirmedChainLength, hasRbfRisk ? "yes" : "no");
      }
   }
}
