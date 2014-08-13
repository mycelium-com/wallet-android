/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet;

import android.util.Log;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl;
import com.mycelium.lt.api.LtApi;
import com.mycelium.lt.api.LtApiClient;
import com.mycelium.lt.api.LtApiClient.Logger;

public class MbwProdEnvironment extends MbwEnvironment {
   /**
    * The thumbprint of the Mycelium certificate. We use this for pinning the
    * server certificate.
    */
   private static final String myceliumThumbprint = "B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3";

   /**
    * Two redundant Mycelium wallet service servers for prodnet
    */
   private static final MyceliumWalletApiImpl.HttpsEndpoint httpsProdnetEndpoint1 = new MyceliumWalletApiImpl.HttpsEndpoint(
         "https://mws1.mycelium.com/mws", myceliumThumbprint);
   private static final MyceliumWalletApiImpl.HttpsEndpoint httpsProdnetEndpoint2 = new MyceliumWalletApiImpl.HttpsEndpoint(
         "https://mws2.mycelium.com/mws", myceliumThumbprint);

   /**
    * The set of endpoints we use for prodnet. The wallet chooses a random
    * endpoint and if it does not respond it round-robins through the list. This
    * way we achieve client side load-balancing and fail-over.
    */
   private static final MyceliumWalletApiImpl.HttpEndpoint[] prodnetServerEndpoints = new MyceliumWalletApiImpl.HttpEndpoint[] {
         httpsProdnetEndpoint1, httpsProdnetEndpoint2 };
   private static final MyceliumWalletApiImpl prodnetApi = new MyceliumWalletApiImpl(prodnetServerEndpoints,
         NetworkParameters.productionNetwork);

   /**
    * Local Trader API for prodnet
    */
   private static final LtApiClient.HttpsEndpoint prodnetLocalTraderDefaultEndpoint = new LtApiClient.HttpsEndpoint(
         "https://lt2.mycelium.com/ltprodnet/", myceliumThumbprint);
   private static final LtApiClient.HttpsEndpoint prodnetLocalTraderFailoverEndpoint = new LtApiClient.HttpsEndpoint(
         "https://lt1.mycelium.com/ltprodnet/", myceliumThumbprint);
   
   private static final LtApiClient prodnetLocalTraderApi = new LtApiClient(prodnetLocalTraderDefaultEndpoint, prodnetLocalTraderFailoverEndpoint, new Logger() {

      @Override
      public void logError(String message, Exception e) {
         Log.e("", message, e);

      }

      @Override
      public void logError(String message) {
         Log.e("", message);

      }
   });
   
   public MbwProdEnvironment(String brand, boolean bitidEnabled){
      super(brand, bitidEnabled);
   }
   
   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.productionNetwork;
   }

   @Override
   public MyceliumWalletApi getMwsApi() {
      return prodnetApi;
   }

   @Override
   public LtApi getLocalTraderApi() {
      return prodnetLocalTraderApi;
   }
}
