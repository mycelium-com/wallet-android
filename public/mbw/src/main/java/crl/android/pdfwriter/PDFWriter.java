//
//  Android PDF Writer
//  http://coderesearchlabs.com/androidpdfwriter
//
//  by Javier Santo Domingo (j-a-s-d@coderesearchlabs.com)
//

package crl.android.pdfwriter;

import android.graphics.Bitmap;

public class PDFWriter {

	private PDFDocument mDocument;
	private IndirectObject mCatalog;
	private Pages mPages;
	private Page mCurrentPage;

	public PDFWriter() {
		newDocument(PaperSize.A4_WIDTH, PaperSize.A4_HEIGHT);
	}

	public PDFWriter(int pageWidth, int pageHeight) {
		newDocument(pageWidth, pageHeight);
	}
	
	private void newDocument(int pageWidth, int pageHeight) {
		mDocument = new PDFDocument();
		mCatalog = mDocument.newIndirectObject();
		mDocument.includeIndirectObject(mCatalog);
		mPages = new Pages(mDocument, pageWidth, pageHeight);
		mDocument.includeIndirectObject(mPages.getIndirectObject());
		renderCatalog();
		newPage();
	}
	
	private void renderCatalog() {
		mCatalog.setDictionaryContent("  /Type /Catalog\n  /Pages " + mPages.getIndirectObject().getIndirectReference() + "\n");
	}
	
	public void newPage() {
		mCurrentPage = mPages.newPage();
		mDocument.includeIndirectObject(mCurrentPage.getIndirectObject());
		mPages.render();
	}
	
	public void setCurrentPage(int pageNumber) {
		mCurrentPage = mPages.getPageAt(pageNumber);
	}
	
	public int getPageCount() {
		return mPages.getCount();
	}
	
	public void setFont(String subType, String baseFont) {
		mCurrentPage.setFont(subType, baseFont);
	}

	public void setFont(String subType, String baseFont, String encoding) {
		mCurrentPage.setFont(subType, baseFont, encoding);
	}
	
	public void addRawContent(String rawContent) {
		mCurrentPage.addRawContent(rawContent);
	}

	public void addText(int leftPosition, int topPositionFromBottom, int fontSize, String text) {
		addText(leftPosition, topPositionFromBottom, fontSize, text, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addText(int leftPosition, int topPositionFromBottom, int fontSize, String text, String transformation) {
		mCurrentPage.addText(leftPosition, topPositionFromBottom, fontSize, text, transformation);
	}

	public void addTextAsHex(int leftPosition, int topPositionFromBottom, int fontSize, String hex) {
		addTextAsHex(leftPosition, topPositionFromBottom, fontSize, hex, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addTextAsHex(int leftPosition, int topPositionFromBottom, int fontSize, String hex, String transformation) {
		mCurrentPage.addTextAsHex(leftPosition, topPositionFromBottom, fontSize, hex, transformation);
	}
	
	public void addLine(int fromLeft, int fromBottom, int toLeft, int toBottom) {
		mCurrentPage.addLine(fromLeft, fromBottom, toLeft, toBottom);
	}
	
	public void addRectangle(int fromLeft, int fromBottom, int toLeft, int toBottom) {
		mCurrentPage.addRectangle(fromLeft, fromBottom, toLeft, toBottom);
	}

	public void addImage(int fromLeft, int fromBottom, Bitmap bitmap) {
		addImage(fromLeft, fromBottom, bitmap, Transformation.DEGREES_0_ROTATION);
	}

	public void addImage(int fromLeft, int fromBottom, Bitmap bitmap, String transformation) {
		final XObjectImage xImage = new XObjectImage(mDocument, bitmap);
		mCurrentPage.addImage(fromLeft, fromBottom, xImage.getWidth(), xImage.getHeight(), xImage, transformation);
	}
	
	public void addImage(int fromLeft, int fromBottom, int toLeft, int toBottom, Bitmap bitmap) {
		addImage(fromLeft, fromBottom, toLeft, toBottom, bitmap, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addImage(int fromLeft, int fromBottom, int toLeft, int toBottom, Bitmap bitmap, String transformation) {
		mCurrentPage.addImage(fromLeft, fromBottom, toLeft, toBottom, new XObjectImage(mDocument, bitmap), transformation);
	}
	
	public void addImageKeepRatio(int fromLeft, int fromBottom, int width, int height, Bitmap bitmap) {
		addImageKeepRatio(fromLeft, fromBottom, width, height, bitmap, Transformation.DEGREES_0_ROTATION);
	}
	
	public void addImageKeepRatio(int fromLeft, int fromBottom, int width, int height, Bitmap bitmap, String transformation) {
		final XObjectImage xImage = new XObjectImage(mDocument, bitmap);
		final float imgRatio = (float) xImage.getWidth() / (float) xImage.getHeight();
		final float boxRatio = (float) width / (float) height;
		float ratio;
		if (imgRatio < boxRatio) {
			ratio = (float) width / (float) xImage.getWidth();
		} else { 
			ratio = (float) height / (float) xImage.getHeight();
		}
		width = (int) (xImage.getWidth() * ratio);
		height = (int) (xImage.getHeight() * ratio);
		mCurrentPage.addImage(fromLeft, fromBottom, width, height, xImage, transformation);
	}
	
	public String asString() {
		mPages.render();
		return mDocument.toPDFString();
	}
}
