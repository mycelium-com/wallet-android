//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

public class Trailer extends Base {

	private int mXRefByteOffset;
	private int mObjectsCount;
	private String mId;
	private Dictionary mTrailerDictionary;
	
	public Trailer() {
		clear();
	}
	
	public void setId(String Value) {
		mId = Value;
	}
	
	public void setCrossReferenceTableByteOffset(int Value) {
		mXRefByteOffset = Value;
	}

	public void setObjectsCount(int Value) {
		mObjectsCount = Value;
	}
	
	private void renderDictionary() {
		mTrailerDictionary.setContent("  /Size " + Integer.toString(mObjectsCount));
		mTrailerDictionary.addNewLine();
		mTrailerDictionary.addContent("  /Root 1 0 R");
		mTrailerDictionary.addNewLine();
		mTrailerDictionary.addContent("  /ID [<" + mId + "> <" + mId + ">]");
		mTrailerDictionary.addNewLine();
	}

	private String render() {
		renderDictionary();
		StringBuilder sb = new StringBuilder();
		sb.append("trailer");
		sb.append("\n");
		sb.append(mTrailerDictionary.toPDFString());
		sb.append("startxref");
		sb.append("\n");
		sb.append(mXRefByteOffset);
		sb.append("\n");
		sb.append("%%EOF");
		sb.append("\n");
		return sb.toString();
	}
	
	@Override
	public String toPDFString() {		
		return render();
	}

	@Override
	public void clear() {
		mXRefByteOffset = 0;
		mTrailerDictionary = new Dictionary();
	}
}
