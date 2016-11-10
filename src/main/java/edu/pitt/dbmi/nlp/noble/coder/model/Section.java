package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.*;

/**
 * The Class Section.
 */
public class Section extends Text{
	private String body,title;
	private int bodyOffset;
	private List<Sentence> sentences;
	private Section parent;
	private List<Section> sections;
	private Document document;
	private Map<String,String> properties;
	
	/**
	 * Gets the sentences.
	 *
	 * @return the sentences
	 */
	public List<Sentence> getSentences() {
		if(sentences == null)
			sentences = new ArrayList<Sentence>();
		return sentences;
	}

	/**
	 * Gets the document.
	 *
	 * @return the document
	 */
	public Document getDocument() {
		return document;
	}

	/**
	 * Sets the document.
	 *
	 * @param document the new document
	 */
	public void setDocument(Document document) {
		this.document = document;
	}

	/**
	 * Adds the sentence.
	 *
	 * @param s the s
	 */
	public void addSentence(Sentence s){
		getSentences().add(s);
		s.setSection(this);
	}

	/**
	 * Adds the sentences.
	 *
	 * @param ss the ss
	 */
	public void addSentences(Collection<Sentence> ss){
		getSentences().addAll(ss);
		for(Sentence s: ss)
			s.setSection(this);
	}

	
	/**
	 * Sets the sentences.
	 *
	 * @param sentences the new sentences
	 */
	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}

	/**
	 * Gets the body.
	 *
	 * @return the body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Sets the body.
	 *
	 * @param body the new body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * Gets the title.
	 *
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Gets the title span.
	 *
	 * @return the title span
	 */
	public Spannable getTitleSpan(){
		Text t = new Text();
		t.setText(title);
		t.setOffset(getTitleOffset());
		return t;
	}
	
	/**
	 * Sets the title.
	 *
	 * @param title the new title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the title offset.
	 *
	 * @return the title offset
	 */
	public int getTitleOffset() {
		return getOffset();
	}

	/**
	 * Sets the title offset.
	 *
	 * @param titleOffset the new title offset
	 */
	public void setTitleOffset(int titleOffset) {
		setOffset(titleOffset);
	}

	/**
	 * Gets the body offset.
	 *
	 * @return the body offset
	 */
	public int getBodyOffset() {
		return bodyOffset;
	}
	
	/**
	 * Gets the body length.
	 *
	 * @return the body length
	 */
	public int getBodyLength() {
		return body.length();
	}
	
	/**
	 * Gets the title length.
	 *
	 * @return the title length
	 */
	public int getTitleLength(){
		return title.length();
	}
	
	/**
	 * Sets the body offset.
	 *
	 * @param bodyOffset the new body offset
	 */
	public void setBodyOffset(int bodyOffset) {
		this.bodyOffset = bodyOffset;
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Text#updateOffset(int)
	 */
	public void updateOffset(int delta){
		super.updateOffset(delta);
		this.bodyOffset += delta;
	}
	
	/**
	 * Gets the sections.
	 *
	 * @return the sections
	 */
	public List<Section> getSections() {
		if(sections == null)
			sections = new ArrayList<Section>();
		return sections;
	}
	
	/**
	 * Sets the sections.
	 *
	 * @param sections the new sections
	 */
	public void setSections(List<Section> sections) {
		this.sections = null;
		addSections(sections);
	}
	
	/**
	 * Adds the section.
	 *
	 * @param s the s
	 */
	public void addSection(Section s){
		getSections().add(s);
		s.setDocument(getDocument());
		s.setParent(this);
	}
	
	/**
	 * Adds the sections.
	 *
	 * @param ss the ss
	 */
	public void addSections(Collection<Section> ss){
		getSections().addAll(ss);
		for(Section s: ss){
			s.setDocument(getDocument());
			s.setParent(this);
		}
	}

	/**
	 * Gets the parent.
	 *
	 * @return the parent
	 */
	public Section getParent() {
		return parent;
	}

	/**
	 * Sets the parent.
	 *
	 * @param parent the new parent
	 */
	public void setParent(Section parent) {
		this.parent = parent;
	}

	/**
	 * Gets the mentions.
	 *
	 * @return the mentions
	 */
	public List<Mention> getMentions(){
		List<Mention> mentions = new ArrayList<Mention>();
		for(Sentence s: getSentences()){
			mentions.addAll(s.getMentions());
		}
		return mentions;
	}
	
	/**
	 * Gets the properties.
	 *
	 * @return the properties
	 */
	public Map<String,String> getProperties() {
		if(properties == null)
			properties = new LinkedHashMap<String, String>();
		return properties;
	}
}
