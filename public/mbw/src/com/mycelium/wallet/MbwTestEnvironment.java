package com.mycelium.wallet;


import java.util.List;

import com.google.common.collect.ImmutableList;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.AddressShort;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl;

public class MbwTestEnvironment extends MbwEnvironment {

   private static final String myceliumThumbprint = "E5:70:76:B2:67:3A:89:44:7A:48:14:81:DF:BD:A0:58:C8:82:72:4F";

   private static final MyceliumWalletApiImpl.HttpsEndpoint httpsTestnetEndpoint = new MyceliumWalletApiImpl.HttpsEndpoint("https://node3.mycelium.com/mwstestnet",
           myceliumThumbprint);

   /**
    * The set of endpoints we use for testnet. The wallet chooses a random
    * endpoint and if it does not respond it round-robins through the list. This
    * way we achieve client side load-balancing and fail-over.
    */
   private static final MyceliumWalletApiImpl.HttpEndpoint[] testnetServerEndpoints = new MyceliumWalletApiImpl.HttpEndpoint[]{httpsTestnetEndpoint};
   private static List<AddressShort> testnetShorteners = ImmutableList.of();
   private static final MyceliumWalletApiImpl testnetApi = new MyceliumWalletApiImpl(testnetServerEndpoints,
           NetworkParameters.testNetwork, testnetShorteners);


   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.testNetwork;
   }

   @Override
   public MyceliumWalletApi getMwsApi() {
      return testnetApi;
   }
}
