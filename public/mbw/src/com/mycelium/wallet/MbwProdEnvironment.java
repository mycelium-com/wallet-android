package com.mycelium.wallet;

import java.util.List;

import com.google.common.collect.ImmutableList;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.AddressShort;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl;
import com.mrd.mbwapi.impl.shorten.BtcTo;
import com.mrd.mbwapi.impl.shorten.Firstbits;

public class MbwProdEnvironment extends MbwEnvironment {
   /**
    * The thumbprint of the Mycelium certificate. We use this for pinning the
    * server certificate.
    */
   private static final String myceliumThumbprint = "B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3";

   /**
    * Two redundant Mycelium wallet service servers for prodnet
    */
   private static final MyceliumWalletApiImpl.HttpsEndpoint httpsProdnetEndpoint1 = new MyceliumWalletApiImpl.HttpsEndpoint("https://mws1.mycelium.com/mws",
           myceliumThumbprint);
   private static final MyceliumWalletApiImpl.HttpsEndpoint httpsProdnetEndpoint2 = new MyceliumWalletApiImpl.HttpsEndpoint("https://mws2.mycelium.com/mws",
           myceliumThumbprint);

   /**
    * The set of endpoints we use for prodnet. The wallet chooses a random
    * endpoint and if it does not respond it round-robins through the list. This
    * way we achieve client side load-balancing and fail-over.
    */
   private static final MyceliumWalletApiImpl.HttpEndpoint[] prodnetServerEndpoints = new MyceliumWalletApiImpl.HttpEndpoint[]{httpsProdnetEndpoint1,
           httpsProdnetEndpoint2};
   private static List<AddressShort> prodnetShorteners = ImmutableList.of(new BtcTo(), new Firstbits());
   private static final MyceliumWalletApiImpl prodnetApi = new MyceliumWalletApiImpl(prodnetServerEndpoints,
           NetworkParameters.productionNetwork, prodnetShorteners);

   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.productionNetwork;
   }

   @Override
   public MyceliumWalletApi getMwsApi() {
      return prodnetApi;
   }
}
