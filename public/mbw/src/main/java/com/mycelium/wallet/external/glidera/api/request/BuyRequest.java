package com.mycelium.wallet.external.glidera.api.request;

import android.support.annotation.NonNull;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.UUID;

public class BuyRequest {
   private final Address destinationAddress;
   private final BigDecimal qty;
   private final UUID priceUuid;
   private final boolean useCurrentPrice;
   private final InetAddress ip;

   /**
    * @param destinationAddress The Bitcoin address which will receive the purchased Bitcoin on the blockchain
    * @param qty                Amount to purchase in Bitcoin (ex. 1.2)
    * @param priceUuid          Identifies the price quote the user is willing to buy Bitcoin for. Price quotes are generated using the
    *                           Buy Prices resource. Price quotes and useCurrentPrice are mutually exclusive, both cannot be used. Price
    *                           quotes have an expiration time and the call will fail if a price quote is expired.
    * @param useCurrentPrice    Boolean value. True if the user wishes to purchase the Bitcoin at market price. Field can't be true if a
    *                           priceUuid is also included.
    * @param ip                 IP Address value. This is required if an end user will be connecting through a third party service
    *                           instead of submitting the call directly from their device.
    */
   public BuyRequest(
           @NonNull Address destinationAddress,
           @NonNull BigDecimal qty, UUID priceUuid, boolean useCurrentPrice, InetAddress
                   ip) {
      Preconditions.checkArgument(priceUuid != null ^ useCurrentPrice);
      this.destinationAddress = destinationAddress;
      this.qty = qty;
      this.priceUuid = priceUuid;
      this.useCurrentPrice = useCurrentPrice;
      this.ip = ip;
   }

   public Address getDestinationAddress() {
      return destinationAddress;
   }

   public BigDecimal getQty() {
      return qty;
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
