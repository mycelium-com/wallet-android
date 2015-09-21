package com.mycelium.wapi.wallet.bip44;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

public class ExternalSignatureProviderProxy {

   private final Map<Integer, ExternalSignatureProvider> signatureProviders;

   public ExternalSignatureProviderProxy(ExternalSignatureProvider... signatureProviders) {
      ImmutableMap.Builder<Integer, ExternalSignatureProvider> mapBuilder
            = new ImmutableMap.Builder<Integer, ExternalSignatureProvider>();
      for (ExternalSignatureProvider signatureProvider : signatureProviders) {
         mapBuilder.put(signatureProvider.getBIP44AccountType(), signatureProvider);
      }

      this.signatureProviders = mapBuilder.build();
   }

   public ExternalSignatureProvider get(int bip44AccountType) {
      return signatureProviders.get(bip44AccountType);
   }

}
