package edu.pitt.dbmi.nlp.noble.coder.processor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.coder.model.*;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

public class ParagraphProcessor implements Processor<Document> {
	private static final String PARAGRAPH = "\\n{2,}";
	private static final String DIVS = "\\-{5,}|_{5,}|={5,}";
	private static final String PARTS = "PARTS?\\s+\\d+(\\s+AND\\s+\\d+)?:";
	private long time;
	
	
	/**
	 * find paragraphs
	 */
	public Document process(Document doc) throws TerminologyException {
		time = System.currentTimeMillis();
		
		// lets try to rely on sections first
		//\\n\\t|\\n\\s{4,}
		Pattern pt = Pattern.compile("("+PARAGRAPH+"|"+DIVS+"|"+PARTS+")",Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
		for(Section section: doc.getSections()){
			int offs = 0;
			int bodyOffset = section.getBodyOffset();
			String text = section.getBody();
			if(text.trim().length() == 0)
				continue;
			Matcher mt = pt.matcher(text);
			String delim = null;
			while(mt.find()){
				delim = mt.group();
				//String txt = text.substring(offs,mt.start());
				//Paragraph pgh = new Paragraph(txt,offs+section.getBodyOffset());
				Paragraph pgh = new Paragraph(doc,offs+bodyOffset,mt.start()+bodyOffset);
				if(delim.matches(PARTS))
					pgh.setPart(delim);
				doc.addParagraph(pgh);
				offs = mt.end();
			}
			// mopup 
			//Paragraph pgh = new Paragraph(text.substring(offs),offs+section.getBodyOffset());
			Paragraph pgh = new Paragraph(doc,offs+bodyOffset,section.getEndPosition());
			if(delim != null && delim.matches(PARTS))
				pgh.setPart(delim);
			doc.addParagraph(pgh);
		}
		
		
		time = System.currentTimeMillis()-time;
		doc.getProcessTime().put(getClass().getSimpleName(),time);
		return doc;
	}

	public long getProcessTime() {
		return time;
	}

}
