package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.Bip44AccountBacking;


public class Bip44PubOnlyAccount extends Bip44Account {

   public Bip44PubOnlyAccount(Bip44AccountContext context, Bip44AccountKeyManager keyManager, NetworkParameters network, Bip44AccountBacking backing, Wapi wapi) {
      super(context, keyManager, network, backing, wapi);
   }

   @Override
   public boolean canSpend() {
      return false;
   }

}
