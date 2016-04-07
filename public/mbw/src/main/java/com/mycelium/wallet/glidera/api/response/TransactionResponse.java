package com.mycelium.wallet.glidera.api.response;

import com.mrd.bitlib.util.Sha256Hash;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class TransactionResponse extends GlideraResponse {
    public enum Type {
        BUY, SELL
    }

    private UUID transactionUuid;
    private Date transactionDate;
    private Type type;
    private BigDecimal price;
    private BigDecimal subtotal;
    private BigDecimal fees;
    private BigDecimal total;
    private BigDecimal qty;
    private String currency;
    private Date estimatedDeliveryDate;
    private Sha256Hash transactionHash;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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

    public Sha256Hash getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(Sha256Hash transactionHash) {
        this.transactionHash = transactionHash;
    }

    public OrderState getStatus() {
        return status;
    }

    public void setStatus(OrderState status) {
        this.status = status;
    }
}
