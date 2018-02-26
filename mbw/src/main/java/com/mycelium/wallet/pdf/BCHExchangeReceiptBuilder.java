package com.mycelium.wallet.pdf;


import crl.android.pdfwriter.PaperSize;

public class BCHExchangeReceiptBuilder {
    private PdfWriter writer;
    private String transactionId;
    private String date;
    private String spendingAmount;
    private String receivingAmount;
    private String receivingAddress;
    private String spendingAccountLabel;

    public BCHExchangeReceiptBuilder setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public BCHExchangeReceiptBuilder setDate(String date) {
        this.date = date;
        return this;
    }

    public BCHExchangeReceiptBuilder setSpendingAmount(String spendingAmount) {
        this.spendingAmount = spendingAmount;
        return this;
    }

    public BCHExchangeReceiptBuilder setReceivingAmount(String receiving) {
        this.receivingAmount = receiving;
        return this;
    }

    public BCHExchangeReceiptBuilder setReceivingAddress(String receivingAddress) {
        this.receivingAddress = receivingAddress;
        return this;
    }

    public BCHExchangeReceiptBuilder setSpendingAccountLabel(String spendingAccountLabel) {
        this.spendingAccountLabel = spendingAccountLabel;
        return this;
    }

    public String build() {
        int pageWidth = PaperSize.EXECUTIVE_WIDTH;
        int pageHeight = PaperSize.EXECUTIVE_HEIGHT;
        writer = new PdfWriter(pageWidth, pageHeight, 20, 20, 20, 20);
        double fromTop = 1.5F;
        writer.addText(1F, fromTop, 16, "Order details:");

        fromTop += 1.5F;
        writer.addText(1F, fromTop, 16, "Convertion From BCH to BTC");


        fromTop += 1.5F;
        writer.addText(1F, fromTop, 16, "TxID: " + transactionId.substring(0, transactionId.length() / 2));
        fromTop += 0.6F;
        writer.addText(1F, fromTop, 16, "           " + transactionId.substring(transactionId.length() / 2));

        fromTop += 1F;
        writer.addText(1F, fromTop, 16, "Date: " + date);

        fromTop += 1F;
        writer.addText(1F, fromTop, 16, "Spending account: " + spendingAccountLabel);

        fromTop += 1F;
        writer.addText(1F, fromTop, 16, "Spending amount: " + spendingAmount);

        fromTop += 1F;
        writer.addText(1F, fromTop, 16, "Receiving amount*: " + receivingAmount);

        fromTop += 1F;
        writer.addText(1F, fromTop, 16, "Receiving address: " + receivingAddress);

        fromTop += 1.5F;
        writer.addText(1F, fromTop, 16, "* Amount estimated ");


        return writer.asString();
    }
}
