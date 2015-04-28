package com.ledger.tbase.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import android.os.Environment;

public class LedgerTAUtils {
	
	public static class LedgerTA {
		
		public LedgerTA(byte[] TA, int spid) {
			this.TA = TA;
			this.spid = spid;
		}
		
		public byte[] getTA() {
			return TA;
		}
		public int getSPID() {
			return spid;
		}
		
		private byte[] TA;
		private int spid;
	}
	
	private static byte[] readFile(File file) {
		if (!file.exists()) {
			return null;
		}
		try {
			byte[] data = new byte[(int)file.length()];
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			dis.readFully(data);
			dis.close();
			return data;
		}
		catch(Exception e) {
			return null;
		}
	}
	
	public static LedgerTA getTA() {
		File pathProd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LedgerTrustlet");
		File trustlet = new File(pathProd, "LWTA-ta.bin");
		File spid = new File(pathProd, "LWTA-spid.bin");
		byte[] ta = readFile(trustlet);
		byte[] spidData = readFile(spid);
		if (ta == null) {
			File pathTest = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LedgerTrustletTest");
			trustlet = new File(pathTest, "LWTA-ta.bin");
			spid = new File(pathTest, "LWTA-spid.bin");
			ta = readFile(trustlet);	
			spidData = readFile(spid);
		}						
		if (ta != null) {
			int spidValue = ((spidData[0] & 0xff) << 24) | ((spidData[1] & 0xff) << 16) | ((spidData[2] & 0xff) << 8) | (spidData[3] & 0xff);
			return new LedgerTA(ta, spidValue);
		}
		else {
			return null;
		}
	}
	
	public static byte[] getTAIssuerPerso() {
		File pathProd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LedgerTrustlet");
		File perso = new File(pathProd, "LWTA-ta-perso.bin");
		return readFile(perso);
	}
	
	public static byte[] getTAExternalUI() {
		File pathProd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LedgerTrustlet");
		File ui = new File(pathProd, "LWTA-ta-ui.bin");
		return readFile(ui);		
	}
	
	public static String getTAExternalUIPath() {
		File pathProd = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LedgerTrustlet");
		File ui = new File(pathProd, "LWTA-ta-ui.bin");
		return ui.getAbsolutePath();
	}	

}
