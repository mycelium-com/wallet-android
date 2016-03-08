package com.mycelium.wallet.glidera;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

public class GlideraUtils {
    public static String formatFiatForDisplay(BigDecimal bigDecimal) {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol("$");
        decimalFormatSymbols.setGroupingSeparator(',');
        decimalFormatSymbols.setMonetaryDecimalSeparator('.');
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setRoundingMode(RoundingMode.HALF_UP);
        ((DecimalFormat) numberFormat).setDecimalFormatSymbols(decimalFormatSymbols);
        return numberFormat.format(bigDecimal);
    }

    public static String formatBtcForDisplay(BigDecimal btc) {
        return "฿" + btc.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
