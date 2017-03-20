//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class Indentifiers {

	private static char[] HexTable = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	private static String calculateMd5(final String s) {
        StringBuffer MD5Str = new StringBuffer();
		try {
	        MessageDigest MD5digester = java.security.MessageDigest.getInstance("MD5");
	        MD5digester.update(s.getBytes());
	        final byte binMD5[] = MD5digester.digest();
	        final int len = binMD5.length;
	        for (int i = 0; i < len; i++) {
	        	MD5Str.append(HexTable[(binMD5[i] >> 4) & 0x0F]); // hi
	        	MD5Str.append(HexTable[(binMD5[i] >> 0) & 0x0F]); // lo
	        }
	    } catch (Exception e) {
			e.printStackTrace();
	    }
	    return MD5Str.toString();
	}

	private static String encodeDate(Date date){
		Calendar c = GregorianCalendar.getInstance();
		c.setTime(date);
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		int hour = c.get(Calendar.HOUR);
		int minute = c.get(Calendar.MINUTE);
		//int second = c.get(Calendar.SECOND);
		int m = c.get(Calendar.DST_OFFSET) / 60000;
		int dts_h = m / 60;
		int dts_m = m % 60;
		String sign = m > 0 ? "+" : "-";
		return String.format(
			"(D:%40d%20d%20d%20d%20d%s%20d'%20d')", year, month, day, hour, minute, sign, dts_h, dts_m
		);
	}
	
	public static String generateId() {
		return calculateMd5(encodeDate(new Date()));
	}
	
	public static String generateId(String data) {
		return calculateMd5(data);
	}
}