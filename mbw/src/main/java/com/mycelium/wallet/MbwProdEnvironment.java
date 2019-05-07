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

public class MbwProdEnvironment extends MbwEnvironment {
   /**
    * The legacy thumbprint of the Mycelium certificate.
    * We used one cert for pinning the server certificate - now we use different certs per endpoint.
    * For access via direct IP we still use the legacy cert to allow non updated wallets to continue
    * to work
    */
   private static final String myceliumLegacyThumbprint = "B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3";

   @Override
   public NetworkParameters getNetwork() {
      return NetworkParameters.productionNetwork;
   }

   /**
    * Local Trader API for prodnet
    */
   private static final ServerEndpoints prodnetLtEndpoints = new ServerEndpoints(new HttpEndpoint[]{
           new HttpsEndpoint("https://lt20.mycelium.com/ltprodnet", "EB:4C:27:A5:A3:8B:DF:E1:34:60:0A:97:57:3F:FA:FF:43:E0:EA:67"),
           new HttpsEndpoint("https://lt10.mycelium.com/ltprodnet", "9E:90:62:24:F7:71:83:FB:B6:B1:D6:4D:C2:78:4A:5D:29:3F:B5:BB"),

           new HttpsEndpoint("https://188.40.73.130/ltprodnet", myceliumLegacyThumbprint), // lt2
           new HttpsEndpoint("https://46.4.101.162/ltprodnet", myceliumLegacyThumbprint), // lt1

           new TorHttpsEndpoint("https://7c7yicf4e3brohwi.onion/ltprodnet", "4E:EE:C3:53:74:92:19:E6:37:EB:1A:2D:E8:21:9B:28:E8:8B:54:6C"),
           new TorHttpsEndpoint("https://az5zxxebeule5hmn.onion/ltprodnet", "62:D7:E2:92:A7:B9:7E:75:C7:B5:34:1E:ED:DB:DC:45:95:70:A0:9E"),
           new TorHttpsEndpoint("https://lodffvexeb72vf2f.onion/ltprodnet", "07:F1:79:DB:52:68:C8:B0:63:05:E8:87:64:D7:1B:57:53:4F:3E:D1"),
           new TorHttpsEndpoint("https://wmywc6g3mknihpq2.onion/ltprodnet", "D2:97:9A:9F:EB:F7:08:D1:89:1B:FC:B5:83:55:BE:1E:6D:B1:AE:E3"),

   }, 0);

   @Override
   public ServerEndpoints getLtEndpoints() {
      return prodnetLtEndpoints;
   }

   /**
    * Wapi
    */
   private static final HttpEndpoint[] prodnetWapiEndpoints = new HttpEndpoint[]{
           // mws 2,6,7,8
           new HttpsEndpoint("https://wapi-htz.mycelium.com:4430", "14:83:CB:96:48:E0:7F:96:D0:C3:78:17:98:6F:E3:72:4C:34:E5:07"),
           new HttpsEndpoint("https://mws20.mycelium.com/wapi", "65:1B:FF:6B:8C:7F:C8:1C:8E:14:77:1E:74:9C:F7:E5:46:42:BA:E0"),


           // Also try to connect to the nodes via a hardcoded IP, in case the DNS has some problems
           new HttpsEndpoint("https://195.201.81.32:4430", "14:83:CB:96:48:E0:7F:96:D0:C3:78:17:98:6F:E3:72:4C:34:E5:07"),   // hetzner load balanced
           new HttpsEndpoint("https://138.201.206.35/wapi", myceliumLegacyThumbprint),   // mws2

           // tor hidden services
           new TorHttpsEndpoint("https://n76y5k3le2zi73bw.onion/wapi", "8D:47:91:A1:EA:9B:CE:E5:A1:9E:38:5B:74:A7:45:0C:88:8F:57:E8"),
           new TorHttpsEndpoint("https://vtuao7psnrsot4tb.onion/wapi", "C5:09:C8:37:84:53:65:EE:8E:22:89:32:8F:86:70:49:AD:0A:53:4D"),
           new TorHttpsEndpoint("https://rztvro6qgydmujfv.onion/wapi", "A4:09:BC:3A:0E:2D:FE:BF:05:FB:9C:65:DC:82:EA:CF:5D:EE:4D:76"),
           new TorHttpsEndpoint("https://slacef5ylu6op7zc.onion/wapi", "EF:62:09:DE:A7:68:15:90:32:93:00:0A:4E:87:05:63:39:B5:87:85"),

   };

   private static final ServerEndpoints prodnetWapiServerEndpoints = new ServerEndpoints(prodnetWapiEndpoints);

   @Override
   public ServerEndpoints getWapiEndpoints() {
      return prodnetWapiServerEndpoints;
   }

   /**
    * Available BlockExplorers
    * <p>
    * The first is the default block explorer if the requested one is not available
    */
   private static final ArrayList<BlockExplorer> prodnetExplorerClearEndpoints = new ArrayList<BlockExplorer>() {{
      add(new BlockExplorer("SBT", "smartbit", "https://www.smartbit.com.au/address/", "https://www.smartbit.com.au/tx/", null, null));
      add(new BlockExplorer("BCI", "blockchain.info", "https://blockchain.info/address/", "https://blockchain.info/tx/", "https://blockchainbdgpzk.onion/address/", "https://blockchainbdgpzk.onion/tx/"));
      add(new BlockExplorer("BTL", "blockTrail", "https://www.blocktrail.com/BTC/address/", "https://www.blocktrail.com/BTC/tx/", null, null));
      add(new BlockExplorer("BPY", "BitPay", "https://insight.bitpay.com/address/", "https://insight.bitpay.com/tx/", null, null));
      add(new BlockExplorer("BEX", "blockExplorer", "http://blockexplorer.com/address/", "http://blockexplorer.com/tx/", null, null));
      add(new BlockExplorer("BCY", "blockCypher", "https://live.blockcypher.com/btc/address/", "https://live.blockcypher.com/btc/tx/", null, null));
      add(new BlockExplorer("TBC", "TradeBlock", "https://tradeblock.com/blockchain/address/", "https://tradeblock.com/blockchain/tx/", null, null));
      add(new BlockExplorer("BLC", "blockonomics.co", "https://www.blockonomics.co/#/search?q=", "https://www.blockonomics.co/api/tx?txid=", null, null));
   }};

   public List<BlockExplorer> getBlockExplorerList() {
      return new ArrayList<>(prodnetExplorerClearEndpoints);
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
