package com.mycelium.wallet.pdf;


import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.StandardFonts;

public class PdfWriter {
    private PDFWriter _writer;
    private int _pageWidth;
    private int _pageHeight;
    private int _marginLeft;
    private int _marginTop;
    private int _marginRight;
    private int _marginBottom;
    protected int _offX;
    protected int _offY;

    public PdfWriter(int pageWidth, int pageHeight, int marginLeft, int marginRight, int marginTop, int marginBottom) {
        _pageWidth = pageWidth;
        _pageHeight = pageHeight;
        _writer = new PDFWriter(pageWidth, pageHeight);
        _marginLeft = marginLeft;
        _marginRight = marginRight;
        _marginTop = marginTop;
        _marginBottom = marginBottom;
        _offX = 0;
        _offY = 0;
        setStandardFont();

        // Add visible Bounding box
        // _writer.addRectangle(1, 1, _pageWidth - 2, _pageHeight - 2);
    }

    public PdfWriter(PdfWriter writer) {
        _pageWidth = writer._pageWidth;
        _pageHeight = writer._pageHeight;
        _writer = writer._writer;
        _marginLeft = writer._marginLeft;
        _marginRight = writer._marginRight;
        _marginTop = writer._marginTop;
        _marginBottom = writer._marginBottom;
        _offX = 0;
        _offY = 0;
    }

    public int getWidth() {
        return _pageWidth - _marginLeft - _marginRight;
    }

    public int getHeight() {
        return _pageHeight - _marginTop - _marginBottom;
    }

    public void setTextColor(double r, double g, double b) {
        _writer.addRawContent(r + " " + g + " " + b + " rg\n");
    }

    public void setLineColor(double r, double g, double b) {
        _writer.addRawContent(r + " " + g + " " + b + " RG\n");
    }

    public void setStandardFont() {
        _writer.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);
    }

    public void setItalicFont() {
        _writer.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ITALIC);
    }

    public void setBoldFont() {
        _writer.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_BOLD);
    }

    public void setMonoFont() {
        _writer.setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER);
    }

    public void addPage() {
        _writer.newPage();
    }

    public void addText(int x, int y, int fontSize, String text) {
        _writer.addText(_marginLeft + _offX + x, _pageHeight - _marginTop - _offY - y - fontSize, fontSize, text);
    }

    public void addText(double cmX, double cmY, int fontSize, String text) {
        addText(translateCmX(cmX), translateCmY(cmY), fontSize, text);
    }

    public void addRectangle(double cmX, double cmY, double cmWidth, double cmHeight) {
        addRectangle(translateCmX(cmX), translateCmY(cmY), translateCmX(cmWidth), translateCmY(cmHeight));
    }

    public void addRectangle(int x, int y, int width, int height) {
        _writer.addRectangle(_marginLeft + _offX + x, _pageHeight - _marginTop - _offY - y, width, -height);
    }

    public void addFilledRectangle(double x, double y, double width, double height) {
        _writer.addFilledRectangle(_marginLeft + _offX + x, _pageHeight - _marginTop - _offY - y, width, -height);
    }

    public void addLine(double cmX1, double cmY1, double cmX2, double cmY2) {
        addLine(translateCmX(cmX1), translateCmY(cmY1), translateCmX(cmX2), translateCmY(cmY2));
    }

    public void addLine(int x1, int y1, int x2, int y2) {
        _writer.addLine(_marginLeft + _offX + x1, _pageHeight - _marginTop - _offY - y1, _marginLeft + _offX + x2,
                _pageHeight - _marginTop - _offY - y2);
    }

    @SuppressWarnings("unused")
    public void addImage(double cmX, double cmY, double cmWidth, double cmHeight, Bitmap bitmap) {
        addImage(translateCmX(cmX), translateCmY(cmY), translateCmX(cmWidth), translateCmY(cmHeight), bitmap);
    }

    public void addImage(int x, int y, int width, int height, Bitmap bitmap) {
        _writer.addImageKeepRatio(_offX + _marginLeft + x, _pageHeight - _marginTop - _offY - y - height, width,
                height, bitmap);
    }

    public void addQrCode(double cmX, double cmY, double cmSize, String url) {
        BitMatrix matrix = getQRCodeMatrix(url);
        int xPos = translateCmX(cmX);
        int yPos = translateCmX(cmY);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        double size = translateCmX(cmSize);
        double boxHeight = size / height;
        double boxWidth = size / width;
        double boxFillHeight = boxHeight + 0.1;
        double boxFillWidth = boxWidth + 0.1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    addFilledRectangle(toTwoDecimalPalces(xPos + boxWidth * x), toTwoDecimalPalces(yPos + boxHeight * y),
                            toTwoDecimalPalces(boxFillWidth), toTwoDecimalPalces(boxFillHeight));
                }
            }
        }
    }

    private double toTwoDecimalPalces(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private static BitMatrix getQRCodeMatrix(String url) {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        try {
            return new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, hints);
        } catch (final WriterException e) {
            throw new RuntimeException(e);
        }
    }

    public String asString() {
        return _writer.asString();
    }

    public int translateCmX(double cmX) {
        return (int) (cmX / 19.2F * getWidth());
    }

    public int translateCmY(double cmY) {
        return (int) (cmY / 27F * getHeight());
    }
}
