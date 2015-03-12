package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.model.Transaction;


public interface ExternalSignatureProvider {
   Transaction sign(StandardTransactionBuilder.UnsignedTransaction unsigned, Bip44AccountExternalSignature forAccount);
}
