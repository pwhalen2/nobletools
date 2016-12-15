package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.ArrayList;
import java.util.List;

public class Paragraph extends Text {
	private String part;
	private Section section;
	private List<Sentence> sentences;
	
	/**
	 * Instantiates a new sentence.
	 *
	 * @param text the text
	 * @param offs the offs
	 * @param type the type
	 */
	public Paragraph(String text,int offs){
		super(text,offs);
	}
	
	/**
	 * initialize with a document
	 * @param doc
	 * @param begin
	 * @param end
	 */
	public Paragraph(Document doc, int begin, int end){
		super(doc,begin,end);
	}
	
	public String getPart() {
		return part;
	}
	public void setPart(String part) {
		this.part = part;
	}
	public boolean isPart(){
		return part != null;
	}
	
	/**
	 * Gets the section.
	 *
	 * @return the section
	 */
	public Section getSection() {
		if(section == null)
			section = getDocument().getSection(this);
		return section;
	}
	
	/**
	 * Gets the sentences.
	 * @return the sentences
	 */
	public List<Sentence> getSentences() {
		if(sentences == null){
			sentences = getDocument().getSentences(this);
		}
		return sentences;
	}
}
