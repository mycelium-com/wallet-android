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
import java.util.List;
import java.util.ArrayList;


public class MbwProdEnvironment extends MbwEnvironment {
   /**
    * The thumbprint of the Mycelium certificate. We use this for pinning the
    * server certificate.
    */
   private static final String myceliumThumbprint = "B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3";


   public MbwProdEnvironment(String brand){
      super(brand);
   }

   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.productionNetwork;
   }


   /**
    * Local Trader API for prodnet
    */
   private static final ServerEndpoints prodnetLtEndpoints = new ServerEndpoints(new HttpEndpoint[]{
         new HttpsEndpoint("https://lt2.mycelium.com/ltprodnet", myceliumThumbprint),
         new HttpsEndpoint("https://lt1.mycelium.com/ltprodnet", myceliumThumbprint),

         new HttpsEndpoint("https://188.40.73.130/ltprodnet", myceliumThumbprint), // lt2
         new HttpsEndpoint("https://46.4.101.162/ltprodnet", myceliumThumbprint), // lt1

         new TorHttpsEndpoint("https://7c7yicf4e3brohwi.onion/ltprodnet", myceliumThumbprint),
         new TorHttpsEndpoint("https://wmywc6g3mknihpq2.onion/ltprodnet", myceliumThumbprint),
         new TorHttpsEndpoint("https://lodffvexeb72vf2f.onion/ltprodnet", myceliumThumbprint),
         new TorHttpsEndpoint("https://az5zxxebeule5hmn.onion/ltprodnet", myceliumThumbprint),
   }, 0);

   @Override
   public ServerEndpoints getLtEndpoints() {
      return  prodnetLtEndpoints;
   }


   /**
    * Wapi
    */
   private static final HttpEndpoint[] prodnetWapiEndpoints = new HttpEndpoint[] {
         // mws 2,6,7
         new HttpsEndpoint("https://mws2.mycelium.com/wapi", myceliumThumbprint),
         new HttpsEndpoint("https://mws6.mycelium.com/wapi", myceliumThumbprint),
         new HttpsEndpoint("https://mws7.mycelium.com/wapi", myceliumThumbprint),
         new HttpsEndpoint("https://mws8.mycelium.com/wapi", myceliumThumbprint),

         // Also try to connect to the nodes via a hardcoded IP, in case the DNS has some problems
         new HttpsEndpoint("https://138.201.206.35/wapi", myceliumThumbprint),   // mws2
         new HttpsEndpoint("https://46.4.101.162/wapi", myceliumThumbprint),  // mws6
         new HttpsEndpoint("https://46.4.3.125/wapi", myceliumThumbprint),     // mws7
         new HttpsEndpoint("https://188.40.73.130/wapi", myceliumThumbprint),     // mws8

         new TorHttpsEndpoint("https://vtuao7psnrsot4tb.onion/wapi", myceliumThumbprint),     // tor hidden services
         new TorHttpsEndpoint("https://n76y5k3le2zi73bw.onion/wapi", myceliumThumbprint),
         new TorHttpsEndpoint("https://slacef5ylu6op7zc.onion/wapi", myceliumThumbprint),
         new TorHttpsEndpoint("https://rztvro6qgydmujfv.onion/wapi", myceliumThumbprint),
   };

   private static final ServerEndpoints prodnetWapiServerEndpoints = new ServerEndpoints(prodnetWapiEndpoints);

   @Override
   public ServerEndpoints getWapiEndpoints() {
      return prodnetWapiServerEndpoints;
   }

   /**
    * Available BlockExplorers
    *
    * The first is the default block explorer if the requested one is not available
    */
   private static final ArrayList <BlockExplorer> prodnetExplorerClearEndpoints = new ArrayList<BlockExplorer>() {{
      add(new BlockExplorer("BCI","blockchain.info","https://blockchain.info/address/","https://blockchain.info/tx/","https://blockchainbdgpzk.onion/address/","https://blockchainbdgpzk.onion/tx/"));
      add(new BlockExplorer("BKR","blockr", "https://btc.blockr.io/address/info/", "https://btc.blockr.io/tx/info/", null, null));
      add(new BlockExplorer("SBT","smartbit", "https://www.smartbit.com.au/address/", "https://www.smartbit.com.au/tx/", null, null));
      add(new BlockExplorer("BTL","blockTrail", "https://www.blocktrail.com/BTC/address/", "https://www.blocktrail.com/BTC/tx/", null, null));
      add(new BlockExplorer("BPY","BitPay", "https://insight.bitpay.com/address/", "https://insight.bitpay.com/tx/", null, null));
      add(new BlockExplorer("BEX","blockExplorer", "http://blockexplorer.com/address/", "http://blockexplorer.com/tx/", null, null));
      add(new BlockExplorer("BAC","bitAccess", "https://search.bitaccess.ca/address/", "https://search.bitaccess.ca/tx/", null, null));
      add(new BlockExplorer("BCY","blockCypher", "https://live.blockcypher.com/btc/address/", "https://live.blockcypher.com/btc/tx/", null, null));
      add(new BlockExplorer("BES","bitEasy", "https://www.biteasy.com/blockchain/addresses/", "https://www.biteasy.com/blockchain/transactions/", null, null));
      add(new BlockExplorer("CPM","coinprism", "https://www.coinprism.info/address/", "https://www.coinprism.info/tx/", null, null));
      add(new BlockExplorer("TBC","TradeBlock", "https://tradeblock.com/blockchain/address/", "https://tradeblock.com/blockchain/tx/", null, null));
      add(new BlockExplorer("BLC","blockonomics.co", "https://www.blockonomics.co/#/search?q=", "https://www.blockonomics.co/api/tx?txid=", null, null));
   }};

   public List<BlockExplorer> getBlockExplorerList() {
      return new ArrayList<BlockExplorer>(prodnetExplorerClearEndpoints);
   }
}
