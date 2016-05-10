package com.mycelium.wallet.external.glidera.api.response;

import java.math.BigDecimal;

public class TransactionLimitsResponse extends GlideraResponse {
    private BigDecimal dailyBuy;
    private BigDecimal dailySell;
    private BigDecimal monthlyBuy;
    private BigDecimal monthlySell;
    private BigDecimal dailyBuyRemaining;
    private BigDecimal dailySellRemaining;
    private BigDecimal monthlyBuyRemaining;
    private BigDecimal monthlySellRemaining;
    private BigDecimal overallBuySell;
    private BigDecimal overallBuySellRemaining;
    private String currency;
    private boolean transactDisabledPendingFirstTransaction;

    public BigDecimal getDailyBuy() {
        return dailyBuy;
    }

    public void setDailyBuy(BigDecimal dailyBuy) {
        this.dailyBuy = dailyBuy;
    }

    public BigDecimal getDailySell() {
        return dailySell;
    }

    public void setDailySell(BigDecimal dailySell) {
        this.dailySell = dailySell;
    }

    public BigDecimal getMonthlyBuy() {
        return monthlyBuy;
    }

    public void setMonthlyBuy(BigDecimal monthlyBuy) {
        this.monthlyBuy = monthlyBuy;
    }

    public BigDecimal getMonthlySell() {
        return monthlySell;
    }

    public void setMonthlySell(BigDecimal monthlySell) {
        this.monthlySell = monthlySell;
    }

    public BigDecimal getDailyBuyRemaining() {
        return dailyBuyRemaining;
    }

    public void setDailyBuyRemaining(BigDecimal dailyBuyRemaining) {
        this.dailyBuyRemaining = dailyBuyRemaining;
    }

    public BigDecimal getDailySellRemaining() {
        return dailySellRemaining;
    }

    public void setDailySellRemaining(BigDecimal dailySellRemaining) {
        this.dailySellRemaining = dailySellRemaining;
    }

    public BigDecimal getMonthlyBuyRemaining() {
        return monthlyBuyRemaining;
    }

    public void setMonthlyBuyRemaining(BigDecimal monthlyBuyRemaining) {
        this.monthlyBuyRemaining = monthlyBuyRemaining;
    }

    public BigDecimal getMonthlySellRemaining() {
        return monthlySellRemaining;
    }

    public void setMonthlySellRemaining(BigDecimal monthlySellRemaining) {
        this.monthlySellRemaining = monthlySellRemaining;
    }

    public BigDecimal getOverallBuySell() {
        return overallBuySell;
    }

    public void setOverallBuySell(BigDecimal overallBuySell) {
        this.overallBuySell = overallBuySell;
    }

    public BigDecimal getOverallBuySellRemaining() {
        return overallBuySellRemaining;
    }

    public void setOverallBuySellRemaining(BigDecimal overallBuySellRemaining) {
        this.overallBuySellRemaining = overallBuySellRemaining;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isTransactDisabledPendingFirstTransaction() {
        return transactDisabledPendingFirstTransaction;
    }

    public void setTransactDisabledPendingFirstTransaction(boolean transactDisabledPendingFirstTransaction) {
        this.transactDisabledPendingFirstTransaction = transactDisabledPendingFirstTransaction;
    }
}
