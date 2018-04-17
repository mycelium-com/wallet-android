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
        int pageWidth = PaperSize.A4_WIDTH;
        int pageHeight = PaperSize.A4_HEIGHT;
        writer = new PdfWriter(pageWidth, pageHeight, 20, 20, 20, 20);
        double fromTop = 1.5F;
        writer.addText(7F, fromTop, 20, "Exchange confirmation");

        fromTop += 1.5F;
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Date and time:");
        writer.setStandardFont();
        writer.addText(3.5F, fromTop, 11, date + ".");

        fromTop += 0.8F;
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Exchange direction:");
        writer.setStandardFont();
        writer.addText(4.3F, fromTop, 11, "From Bitcoin Cash (BCH) to Bitcoin (BTC).");

        fromTop += 0.8F;
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Blockchain Transaction ID:");
        writer.setStandardFont();
        fromTop += 0.4F;
        writer.addText(1F, fromTop, 11, transactionId);

        fromTop += 0.8F;
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Spending account:");
        writer.setStandardFont();
        writer.addText(4.1F, fromTop, 11, spendingAccountLabel + ".");

        fromTop += 0.8F;
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Spending amount:");
        writer.setStandardFont();
        writer.addText(4.1F, fromTop, 11, spendingAmount + ".");

        fromTop += 0.8F;
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Receiving amount*:");
        writer.setStandardFont();
        writer.addText(4.3F, fromTop, 11, receivingAmount + ".");
        fromTop += 0.5F;
        writer.setTextColor(1, 0, 0);
        writer.addText(1F, fromTop, 10, "*Receiving amount is estimated due to the high volatility of the cryptomarket.");

        fromTop += 0.8F;
        writer.setTextColor(0, 0, 0);
        writer.setBoldFont();
        writer.addText(1F, fromTop, 11, "Receiving address:");
        writer.setStandardFont();
        writer.addText(4.1F, fromTop, 11, receivingAddress + ".");

        fromTop += 1F;
        writer.setItalicFont();
        writer.addText(1F, fromTop, 11, "BTC will be sent to you after 12 confirmations on your BCH transaction.");

        return writer.asString();
    }
}
