package edu.pitt.dbmi.nlp.noble.coder.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.model.Section;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.tools.SentenceDetector;
import edu.pitt.dbmi.nlp.noble.tools.SynopticReportDetector;

public class SentenceProcessor implements Processor<Document> {
	private static final String PROSE_PATTERN = ".*\\b[a-z]+(\\.|\\?|!)\\s+[A-Z][a-z]+\\b.*";
	private long time;
	
	
	/**
	 * add sentences to a document
	 */
	public Document process(Document doc) {
		time = System.currentTimeMillis();
		
		int offset = 0, strOffset = 0;
		StringBuffer str = new StringBuffer();
		String last = null;
		for(String s: doc.getText().split("\n")){
			// check if this sentence does not need to be merged
			// with the previous one, lets save it
			if(!mergeLines(last,s)){
				// save previous region
				if(str.toString().trim().length() > 0){
					// if multiline buffer, then do prose parsing
					if(str.toString().trim().contains("\n") || str.toString().trim().matches(PROSE_PATTERN)){
						parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_PROSE);
					}else{
						parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_LINE);
					}
					// start the counter again
					str = new StringBuffer();
					strOffset = offset;
				}
			}
			// add this line to the next buffer
			str.append(s+"\n");
			offset += s.length()+1;
			last  = s;
		}
		// take care of the last sentence
		if(str.length() > 0){
			if(str.toString().trim().contains("\n") || str.toString().trim().matches(PROSE_PATTERN)){
				parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_PROSE);
			}else{
				parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_LINE);
			}
		}
		time = System.currentTimeMillis() - time;
		doc.getProcessTime().put(getClass().getSimpleName(),time);
		return doc;
	}
	
	
	/**
	 * parse sentences for a region of text based on type.
	 *
	 * @param doc the doc
	 * @param text the text
	 * @param offset the offset
	 * @param type the type
	 */
	private void parseSentences(Document doc, String text, int offset, String type){
		// if sentence starts with lots of spaces or bullets 
		// old pattern
		Pattern p = Pattern.compile("^(([A-Z]?[0-9-\\)]{0,2}\\.?)?\\s+)\\w.*",Pattern.DOTALL|Pattern.MULTILINE);
	
		Matcher m = p.matcher(text);
		if(m.matches()){
			String prefix = m.group(1);
			if(prefix.trim().length()>0){
				text = text.substring(prefix.length());
				offset = offset + prefix.length();
			}
		}
		
		// start adding sentences
		List<Sentence> sentences = new ArrayList<Sentence>();
		if(Sentence.TYPE_PROSE.equals(type)){
			sentences = SentenceDetector.getSentences(text,offset);
		}else{
			Sentence s = new Sentence(text,offset,Sentence.TYPE_LINE);
			if(isDivider(s))
				s.setSentenceType(Sentence.TYPE_DIVIDER);
			
			parseProperties(doc,text);
			if(SynopticReportDetector.detect(text)){
				s.setSentenceType(Sentence.TYPE_WORKSHEET);
			}
			sentences.add(s);
		}
			
		// add to section
		//should it really be here???? Why not?
		if(!sentences.isEmpty()){
			Section sec = doc.getSection(sentences.get(0));
			if(sec != null){
				Sentence s = sentences.get(0);
				if(s.contains(sec.getTitleSpan())){
					//OK, this sentence contains header, do we need to 
					// parse it fruther?
					int en = sec.getTitleSpan().getEndPosition()-offset;
					String first = s.getText().substring(0,en);
					String rest = s.getText().substring(en);
					if(rest.trim().length() > 0){
						// there is more after header, need to break it in two
						sentences.remove(s);
						sentences.add(0,new Sentence(rest,offset+en,s.getSentenceType()));
						sentences.add(0,new Sentence(first,offset,Sentence.TYPE_HEADER));
						
					}else{
						// just set this sentence as header
						s.setSentenceType(Sentence.TYPE_HEADER);
					}
				}
				//sec.addSentences(sentences);
			}
		}
		

		
		
		// add sentences to document
		doc.addSentences(sentences);
	}
	

	private boolean isDivider(Sentence s) {
		return s.getText().trim().matches("(\\-{5,}|_{5,}|={5,})");
	}


	/**
	 * parse properties in a document.
	 *
	 * @param doc the doc
	 * @param text the text
	 */
	private void parseProperties(Document doc, String text){
		Pattern p = Pattern.compile("([A-Z][A-Za-z /]{3,25})(?:\\.{2,}|\\:)(.{2,25})");
		Matcher m = p.matcher(text);
		while(m.find()){
			doc.getProperties().put(m.group(1).trim(),m.group(2).trim());
		}
	}
	
	/**
	 * Merge lines.
	 *
	 * @param last the last
	 * @param s the s
	 * @return true, if successful
	 */
	private boolean mergeLines(String last, String s) {
		if(last == null)
			return false;
		// if previous item is worksheet ..
		if(SynopticReportDetector.detect(last))
			return false;
		
		// if previous sentence ends with a lower case word or digit or comma
		if(last.matches(".+\\s([A-Z]?[a-z]+|\\d+),?") && s.matches("\\s*([A-Z]?[a-z]+)\\b.+")){
			return true;
		}
		return false;
	}

	

	public long getProcessTime() {
		return time;
	}

}
