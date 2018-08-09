package com.mycelium.wapi.wallet.btc.bip44;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.btc.Bip44BtcAccountBacking;


public class Bip44PubOnlyBtcAccount extends Bip44BtcAccount {
   public Bip44PubOnlyBtcAccount(Bip44AccountContext context, Bip44AccountKeyManager keyManager, NetworkParameters network, Bip44BtcAccountBacking backing, Wapi wapi) {
      super(context, keyManager, network, backing, wapi);
   }

   @Override
   public boolean canSpend() {
      return false;
   }
}
