//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class XObjectImage {

	public static final int BITSPERCOMPONENT_8 = 8;
	public static final String DEVICE_RGB = "/DeviceRGB";

	public static boolean INTERPOLATION = false;
	public static int BITSPERCOMPONENT = BITSPERCOMPONENT_8;
	public static String COLORSPACE = DEVICE_RGB;
	
	public static int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;
	public static String ENCODING = "ISO-8859-1";

	private static int mImageCount = 0;
	
	private PDFDocument mDocument;
	private IndirectObject mIndirectObject;
	private int mDataSize = 0;
	private int mWidth = -1;
	private int mHeight = -1;
	private String mName = "";
	private String mId = "";
	private String mProcessedImage = "";
	
	public XObjectImage(PDFDocument document, Bitmap bitmap) {
		mDocument = document;
		mProcessedImage = processImage(configureBitmap(bitmap));
		mId = Indentifiers.generateId(mProcessedImage);
		mName = "/img" + (++mImageCount);
	}
	
	public void appendToDocument() {
		mIndirectObject = mDocument.newIndirectObject();
		mDocument.includeIndirectObject(mIndirectObject);
		mIndirectObject.addDictionaryContent(
			" /Type /XObject\n" +
			" /Subtype /Image\n" +
			" /Filter [/ASCII85Decode /FlateDecode]\n" +
			" /Width " + mWidth + "\n" +
			" /Height " + mHeight + "\n" +
			" /BitsPerComponent " + Integer.toString(BITSPERCOMPONENT) + "\n" +
			" /Interpolate " + Boolean.toString(INTERPOLATION) + "\n" +
			" /ColorSpace " + DEVICE_RGB + "\n" +
			" /Length " + mProcessedImage.length() + "\n"
		);
		mIndirectObject.addStreamContent(mProcessedImage);
	}
	
	private Bitmap configureBitmap(Bitmap bitmap) {
		final Bitmap img = bitmap.copy(Config.ARGB_8888, false);
		if (img != null) {
			mWidth = img.getWidth();
			mHeight = img.getHeight();
			mDataSize = mWidth * mHeight * 3;
		}
		return img;
	}
	
	private byte[] getBitmapData(Bitmap bitmap) {
		byte[] data = null;
		if (bitmap != null) {
			data = new byte[mDataSize];
			int intColor;
			int offset = 0;
			for (int y = 0; y < mHeight; y++) {
				for (int x = 0; x < mWidth; x++) {
					intColor = bitmap.getPixel(x, y);
					data[offset++] = (byte) ((intColor>>16) & 0xFF); 
					data[offset++] = (byte) ((intColor>>8) & 0xFF); 
					data[offset++] = (byte) ((intColor>>0) & 0xFF); 
				}
			}
		}
		return data;
	}
	
	private boolean deflateImageData(ByteArrayOutputStream baos, byte[] data) {
		if (data != null) {
			Deflater deflater = new Deflater(COMPRESSION_LEVEL);
			DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater);
			try {
				dos.write(data);
				dos.close();
				deflater.end();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	private String encodeImageData(ByteArrayOutputStream baos) {
		ByteArrayOutputStream sob = new ByteArrayOutputStream();
      ASCII85Encoder enc85 = new ASCII85Encoder(sob);
		try {
			int i = 0;
			for (byte b : baos.toByteArray()) {
				enc85.write(b);
				if (i++ == 255) {
					sob.write((int)'\n');
					i = 0;
				}
			}
			return sob.toString(ENCODING);
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
		   try {
            enc85.close();
         } catch (IOException e) {
            // Ignore
         }
		}
		return "";
	}
	
	private String processImage(Bitmap bitmap) {
   	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
   	    if (deflateImageData(baos, getBitmapData(bitmap))) {
   			return encodeImageData(baos);
   	    }
   	    return null;
	}
	
	public String asXObjectReference() {
		return mName + " " + mIndirectObject.getIndirectReference();
	}

	public String getName() {
		return mName;
	}

	public String getId() {
		return mId;
	}

	public int getWidth() {
		return mWidth;
	}
	
	public int getHeight() {
		return mHeight;		
	}
}
