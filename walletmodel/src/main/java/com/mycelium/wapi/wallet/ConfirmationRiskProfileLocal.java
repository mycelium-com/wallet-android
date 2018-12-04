package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.ConfirmationRiskProfile;

public class ConfirmationRiskProfileLocal extends ConfirmationRiskProfile {
   public final boolean isDoubleSpend;

   public ConfirmationRiskProfileLocal(int unconfirmedChainLength, boolean hasRbfRisk, boolean isDoubleSpend) {
      super(unconfirmedChainLength, hasRbfRisk);
      this.isDoubleSpend = isDoubleSpend;
   }
}
