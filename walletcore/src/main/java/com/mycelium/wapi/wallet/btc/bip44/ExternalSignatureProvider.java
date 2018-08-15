package com.mycelium.wapi.wallet.btc.bip44;

import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Transaction;

/**
 * Hardware wallets provide signatures so accounts can work without the private keys themselves.
 */
public interface ExternalSignatureProvider {
   Transaction getSignedTransaction(UnsignedTransaction unsigned, Bip44BtcAccountExternalSignature forAccount);
   int getBIP44AccountType();
}
