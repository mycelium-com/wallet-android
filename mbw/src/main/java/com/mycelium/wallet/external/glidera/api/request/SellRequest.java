package com.mycelium.wallet.external.glidera.api.request;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;

import java.net.InetAddress;
import java.util.UUID;

public class SellRequest {
   private final Address refundAddress;
   private final String signedTransaction;
   private final UUID priceUuid;
   private final boolean useCurrentPrice;
   private final InetAddress ip;

   /**
    * @param refundAddress     The Bitcoin address which will receive the refunded Bitcoin in the event of an error
    * @param signedTransaction The signed raw transaction to send Glidera the Bitcoin to sell. Glidera will publish this transaction to
    *                          the blockchain after validation. Wallet partners should NOT publish the transaction. In the unlikely
    *                          event an error occurs after Glidera returns a success code and publishes the signed transaction, Bitcoin
    *                          will be refunded to the refund address minus a miner tip. If a sell fails due to user validation the
    *                          wallet partner should spend the outputs again to reclaim the Bitcoin.
    * @param priceUuid         Identifies the price quote the user is willing to sell Bitcoin for. Price quotes are generated using the
    *                          Sell Prices resource. Price quotes and useCurrentPrice are mutually exclusive, only one can be used.
    *                          Price quotes have an expiration time.
    * @param useCurrentPrice   True if the user wishes to sell the Bitcoin at market price. Field can't be true if a priceUuid is also
    *                          included
    * @param ip                IP Address value. This is required if an end user will be connecting through a third party service
    *                          instead of submitting the call directly from their device.
    */
   public SellRequest(Address refundAddress, String signedTransaction, UUID priceUuid, boolean useCurrentPrice, InetAddress ip) {
      Preconditions.checkArgument(priceUuid != null ^ useCurrentPrice);
      this.refundAddress = refundAddress;
      this.signedTransaction = signedTransaction;
      this.priceUuid = priceUuid;
      this.useCurrentPrice = useCurrentPrice;
      this.ip = ip;
   }

   public Address getRefundAddress() {
      return refundAddress;
   }

   public String getSignedTransaction() {
      return signedTransaction;
   }

   public UUID getPriceUuid() {
      return priceUuid;
   }

   public boolean isUseCurrentPrice() {
      return useCurrentPrice;
   }

   public InetAddress getIp() {
      return ip;
   }
}
