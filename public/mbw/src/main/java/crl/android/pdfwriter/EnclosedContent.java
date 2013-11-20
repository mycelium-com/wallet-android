//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

public class EnclosedContent extends Base {

	private String mBegin;
	private String mEnd;
	protected StringBuilder mContent;
	
	public EnclosedContent() {
		clear();
	}
	
	public void setBeginKeyword(String Value, boolean NewLineBefore, boolean NewLineAfter) {
		if (NewLineBefore)
			mBegin = "\n" + Value;
		else
			mBegin = Value;
		if (NewLineAfter)
			mBegin += "\n";
	}

	public void setEndKeyword(String Value, boolean NewLineBefore, boolean NewLineAfter) {		
		if (NewLineBefore)
			mEnd = "\n" + Value;
		else
			mEnd = Value;
		if (NewLineAfter)
			mEnd += "\n";
	}
	
	public boolean hasContent() {
		return mContent.length() > 0;
	}

	public void setContent(String Value) {
		clear();
		mContent.append(Value);
	}
	
	public String getContent() {
		return mContent.toString();
	}

	public void addContent(String Value) {
		mContent.append(Value);
	}
	
	public void addNewLine() {
		mContent.append("\n");
	}

	public void addSpace() {
		mContent.append(" ");
	}
	
	@Override
	public void clear() {
		mContent = new StringBuilder();
	}
	
	@Override
	public String toPDFString() {
		return mBegin + mContent.toString() + mEnd;
	}

}
