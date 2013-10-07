package com.mycelium.wallet;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.MyceliumWalletApi;

public abstract class MbwEnvironment {
   public abstract NetworkParameters getNetwork();

   public abstract MyceliumWalletApi getMwsApi();
}
