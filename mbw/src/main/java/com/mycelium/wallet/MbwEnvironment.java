package com.mycelium.wallet;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.external.BuySellServiceDescriptor;

import java.util.List;
import java.util.Map;

public abstract class MbwEnvironment {
   static MbwEnvironment verifyEnvironment() {
      if (Utils.isProdnet()) {
         return new MbwProdEnvironment();
      } else {
         return new MbwTestnet4Environment();
      }
   }

   public abstract NetworkParameters getNetwork();
   public abstract ServerEndpoints getLtEndpoints();
   public abstract ServerEndpoints getWapiEndpoints();
   public abstract Map<String, List<BlockExplorer>> getBlockExplorerMap();
   public abstract List<BuySellServiceDescriptor> getBuySellServices();
}
