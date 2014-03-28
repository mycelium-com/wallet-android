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

public class MbwTestEnvironment extends MbwEnvironment {

   private static final String myceliumThumbprint = "E5:70:76:B2:67:3A:89:44:7A:48:14:81:DF:BD:A0:58:C8:82:72:4F";

   private static final MyceliumWalletApiImpl.HttpsEndpoint httpsTestnetEndpoint = new MyceliumWalletApiImpl.HttpsEndpoint(
         "https://node3.mycelium.com/mwstestnet", myceliumThumbprint);

//   private static final MyceliumWalletApiImpl.HttpEndpoint httpsTestnetEndpoint = new MyceliumWalletApiImpl.HttpEndpoint(
//         "http://192.168.1.139:8086");

   /**
    * The set of endpoints we use for testnet. The wallet chooses a random
    * endpoint and if it does not respond it round-robins through the list. This
    * way we achieve client side load-balancing and fail-over.
    */
   private static final MyceliumWalletApiImpl.HttpEndpoint[] testnetServerEndpoints = new MyceliumWalletApiImpl.HttpEndpoint[] { httpsTestnetEndpoint };
   private static final MyceliumWalletApiImpl testnetApi = new MyceliumWalletApiImpl(testnetServerEndpoints,
         NetworkParameters.testNetwork);

   /**
    * Local Trader API for testnet
    */
//   private static final LtApiClient.HttpEndpoint testnetLocalTraderEndpoint = new LtApiClient.HttpEndpoint(
//         "http://192.168.1.139:8089/trade/");

   private static final LtApiClient.HttpsEndpoint testnetLocalTraderEndpoint = new LtApiClient.HttpsEndpoint(
         "https://node3.mycelium.com/lttestnet/", myceliumThumbprint);

   private static final LtApiClient testnetLocalTraderApi = new LtApiClient(testnetLocalTraderEndpoint, new Logger() {

      @Override
      public void logError(String message, Exception e) {
         Log.e("", message, e);

      }

      @Override
      public void logError(String message) {
         Log.e("", message);

      }
   });

   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.testNetwork;
   }

   @Override
   public MyceliumWalletApi getMwsApi() {
      return testnetApi;
   }

   @Override
   public LtApi getLocalTraderApi() {
      return testnetLocalTraderApi;
   }
}
