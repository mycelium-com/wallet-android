package com.mycelium.wallet.external.glidera.api.response;

import com.google.gson.GsonBuilder;

public class GlideraResponse {
   @Override
   public String toString() {
      return new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(this).toString();
   }
}
