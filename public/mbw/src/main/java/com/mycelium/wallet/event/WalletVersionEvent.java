package com.mycelium.wallet.event;

import com.google.common.base.Optional;

import com.mrd.mbwapi.api.WalletVersionResponse;

public class WalletVersionEvent {
   public final Optional<WalletVersionResponse> response;

   public WalletVersionEvent() {
      response = Optional.absent();
   }

   public WalletVersionEvent(WalletVersionResponse response) {
      this.response = Optional.of(response);
   }
}
