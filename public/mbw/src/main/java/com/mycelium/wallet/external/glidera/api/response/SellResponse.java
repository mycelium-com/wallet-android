package com.mycelium.wallet.external.glidera.api.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class SellResponse extends GlideraResponse {
   private UUID transactionUuid;
   private Date transactionDate;
   private BigDecimal price;
   private BigDecimal subtotal;
   private BigDecimal fees;
   private BigDecimal total;
   private BigDecimal qty;
   private String currency;
   private Date estimatedDeliveryDate;
   private OrderState status;

   public UUID getTransactionUuid() {
      return transactionUuid;
   }

   public void setTransactionUuid(UUID transactionUuid) {
      this.transactionUuid = transactionUuid;
   }

   public Date getTransactionDate() {
      return transactionDate;
   }

   public void setTransactionDate(Date transactionDate) {
      this.transactionDate = transactionDate;
   }

   public BigDecimal getPrice() {
      return price;
   }

   public void setPrice(BigDecimal price) {
      this.price = price;
   }

   public BigDecimal getSubtotal() {
      return subtotal;
   }

   public void setSubtotal(BigDecimal subtotal) {
      this.subtotal = subtotal;
   }

   public BigDecimal getFees() {
      return fees;
   }

   public void setFees(BigDecimal fees) {
      this.fees = fees;
   }

   public BigDecimal getTotal() {
      return total;
   }

   public void setTotal(BigDecimal total) {
      this.total = total;
   }

   public BigDecimal getQty() {
      return qty;
   }

   public void setQty(BigDecimal qty) {
      this.qty = qty;
   }

   public String getCurrency() {
      return currency;
   }

   public void setCurrency(String currency) {
      this.currency = currency;
   }

   public Date getEstimatedDeliveryDate() {
      return estimatedDeliveryDate;
   }

   public void setEstimatedDeliveryDate(Date estimatedDeliveryDate) {
      this.estimatedDeliveryDate = estimatedDeliveryDate;
   }

   public OrderState getStatus() {
      return status;
   }

   public void setStatus(OrderState status) {
      this.status = status;
   }
}
