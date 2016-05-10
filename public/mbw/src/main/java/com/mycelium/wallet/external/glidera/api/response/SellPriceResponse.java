package com.mycelium.wallet.external.glidera.api.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class SellPriceResponse extends GlideraResponse {
    private BigDecimal qty;
    private BigDecimal price;
    private BigDecimal subtotal;
    private BigDecimal fees;
    private BigDecimal total;
    private String currency;
    private Date expires;
    private UUID priceUuid;

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public UUID getPriceUuid() {
        return priceUuid;
    }

    public void setPriceUuid(UUID priceUuid) {
        this.priceUuid = priceUuid;
    }
}
