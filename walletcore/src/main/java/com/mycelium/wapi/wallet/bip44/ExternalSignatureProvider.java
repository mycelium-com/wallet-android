package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Transaction;

/**
 * Hardware wallets provide signatures so accounts can work without the private keys themselves.
 */
public interface ExternalSignatureProvider {
   Transaction getSignedTransaction(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount);
   int getBIP44AccountType();
}
