//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

public class CrossReferenceTable extends List {

	private int mObjectNumberStart;
	
	public CrossReferenceTable() {
		super();
		clear();
	}
	
	public void setObjectNumberStart(int Value) {
		mObjectNumberStart = Value;
	}
	
	public int getObjectNumberStart() {
		return mObjectNumberStart;
	}
	
	private String getObjectsXRefInfo() {
		return renderList();
	}
	
	public void addObjectXRefInfo(int ByteOffset, int Generation, boolean InUse) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%010d", ByteOffset));
		sb.append(" ");
		sb.append(String.format("%05d", Generation));
		if (InUse) {
			sb.append(" n ");
		} else {
			sb.append(" f ");
		}
		sb.append("\r\n");
		mList.add(sb.toString());
	}

	private String render() {
		StringBuilder sb = new StringBuilder();
		sb.append("xref");
		sb.append("\r\n");
		sb.append(mObjectNumberStart);
		sb.append(" ");
		sb.append(mList.size());
		sb.append("\r\n");
		sb.append(getObjectsXRefInfo());
		return sb.toString(); 
	}	
	
	@Override
	public String toPDFString() {
		return render();
	}

	@Override
	public void clear() {
		super.clear();
		addObjectXRefInfo(0, 65536, false); // free objects linked list head
		mObjectNumberStart = 0;
	}

}
