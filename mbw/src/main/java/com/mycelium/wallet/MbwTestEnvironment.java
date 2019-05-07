/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.HttpsEndpoint;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.net.TorHttpsEndpoint;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.external.BuySellServiceDescriptor;
import com.mycelium.wallet.external.LocalTraderServiceDescription;
import com.mycelium.wallet.external.SimplexServiceDescription;

import java.util.ArrayList;
import java.util.List;

public class MbwTestEnvironment extends MbwEnvironment {
   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.testNetwork;
   }

   /**
    * Local Trader API for testnet
    */
   private static final ServerEndpoints testnetLtEndpoints = new ServerEndpoints(new HttpEndpoint[]{
           new HttpsEndpoint("https://mws30.mycelium.com/lttestnet", "ED:C2:82:16:65:8C:4E:E1:C7:F6:A2:2B:15:EC:30:F9:CD:48:F8:DB"),
           new TorHttpsEndpoint("https://grrhi6bwwpiarsfl.onion/lttestnet", "D0:09:70:40:98:71:E0:0E:62:08:1A:36:4C:BC:C7:2E:51:40:50:4C"),
   });

   @Override
   public ServerEndpoints getLtEndpoints() {
      return testnetLtEndpoints;
   }

   /**
    * Wapi
    */
   private static final ServerEndpoints testnetWapiEndpoints = new ServerEndpoints(new HttpEndpoint[]{
           new HttpsEndpoint("https://mws30.mycelium.com/wapitestnet", "ED:C2:82:16:65:8C:4E:E1:C7:F6:A2:2B:15:EC:30:F9:CD:48:F8:DB"),
           new TorHttpsEndpoint("https://ti4v3ipng2pqutby.onion/wapitestnet", "75:3E:8A:87:FA:95:9F:C6:1A:DB:2A:09:43:CE:52:74:27:B1:80:4B"),
   });

   @Override
   public ServerEndpoints getWapiEndpoints() {
      return testnetWapiEndpoints;
   }

   /**
    * Available BlockExplorers
    * <p>
    * The first is the default block explorer if the requested one is not available
    */
   private static final List<BlockExplorer> testnetExplorerClearEndpoints = new ArrayList<BlockExplorer>() {{
      add(new BlockExplorer("SBT", "smartbit", "https://testnet.sandbox.smartbit.com.au/address/", "https://testnet.smartbit.com.au/tx/", null, null));
      add(new BlockExplorer("BTL", "blockTrail", "https://www.blocktrail.com/tBTC/address/", "https://www.blocktrail.com/tBTC/tx/", null, null));
      add(new BlockExplorer("BPY", "BitPay", "https://test-insight.bitpay.com/address/", "https://test-insight.bitpay.com/tx/", null, null));
      add(new BlockExplorer("BEX", "blockExplorer", "http://blockexplorer.com/testnet/address/", "https://blockexplorer.com/testnet/tx/", null, null));
      add(new BlockExplorer("BCY", "blockCypher", "https://live.blockcypher.com/btc-testnet/address/", "https://live.blockcypher.com/btc-testnet/tx/", null, null));
   }};

   public List<BlockExplorer> getBlockExplorerList() {
      return new ArrayList<BlockExplorer>(testnetExplorerClearEndpoints);
   }

   public List<BuySellServiceDescriptor> getBuySellServices(){
      return new ArrayList<BuySellServiceDescriptor>() {{
         add(new SimplexServiceDescription());
         // add(new CreditCardBuyServiceDescription());
         add(new LocalTraderServiceDescription());
         // add(new ChangellyServiceDescription());
      }};
   }
}
