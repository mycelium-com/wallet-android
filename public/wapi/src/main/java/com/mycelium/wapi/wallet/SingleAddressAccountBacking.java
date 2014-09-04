package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.single.SingleAddressAccountContext;

public interface SingleAddressAccountBacking extends AccountBacking {

   void updateAccountContext(SingleAddressAccountContext context);

}