//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

public class Dictionary extends EnclosedContent {
	
	public Dictionary() {
		super();
		setBeginKeyword("<<",false,true);
		setEndKeyword(">>",false,true);
	}
	
}
