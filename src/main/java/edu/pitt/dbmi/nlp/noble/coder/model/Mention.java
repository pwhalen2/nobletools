package edu.pitt.dbmi.nlp.noble.coder.model;

import edu.pitt.dbmi.nlp.noble.terminology.*;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.ConText;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

import java.util.*;

/**
 * This object represents a concept mention in text.
 *
 * @author tseytlin
 */
public class Mention implements Spannable, Comparable<Mention> {
	private Concept concept;
	private List<Annotation> annotations;
	private Sentence sentence;
	private Map<String,Modifier> modifiers;
	
	
	/**
	 * Gets the concept.
	 *
	 * @return the concept
	 */
	public Concept getConcept() {
		return concept;
	}
	
	/**
	 * Sets the concept.
	 *
	 * @param concept the new concept
	 */
	public void setConcept(Concept concept) {
		this.concept = concept;
	}
	
	/**
	 * Gets the annotations.
	 *
	 * @return the annotations
	 */
	public List<Annotation> getAnnotations() {
		if(annotations == null)
			annotations = new ArrayList<Annotation>();
		return annotations;
	}
	
	/**
	 * Sets the annotations.
	 *
	 * @param annotations the new annotations
	 */
	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}
	
	/**
	 * Gets the sentence.
	 *
	 * @return the sentence
	 */
	public Sentence getSentence() {
		return sentence;
	}
	
	/**
	 * Sets the sentence.
	 *
	 * @param sentence the new sentence
	 */
	public void setSentence(Sentence sentence) {
		this.sentence = sentence;
		for(Annotation a: getAnnotations()){
			if(!a.isOffsetUpdated())
				a.updateOffset(sentence.getOffset());
		}
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#getText()
	 */
	public String getText(){
		StringBuffer b = new StringBuffer();
		
		for(Annotation a: getAnnotations()){
			b.append(" "+a.getText());
		}
		
		return b.toString().trim();
	}
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return concept.getName();
	}
	
	/**
	 * Gets the code.
	 *
	 * @return the code
	 */
	public String getCode() {
		return concept.getCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return getText();
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#getStartPosition()
	 */
	public int getStartPosition() {
		return !getAnnotations().isEmpty()?getAnnotations().get(0).getStartPosition():0;
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#getEndPosition()
	 */
	public int getEndPosition() {
		return !getAnnotations().isEmpty()?getAnnotations().get(getAnnotations().size()-1).getEndPosition():0;
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#contains(edu.pitt.dbmi.nlp.noble.coder.model.Spannable)
	 */
	public boolean contains(Spannable s) {
		return getStartPosition() <= s.getStartPosition() && s.getEndPosition() <= getEndPosition();
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#intersects(edu.pitt.dbmi.nlp.noble.coder.model.Spannable)
	 */
	public boolean intersects(Spannable s) {
		return !(getEndPosition() < s.getStartPosition() || s.getEndPosition() < getStartPosition());
	}
	
	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#before(edu.pitt.dbmi.nlp.noble.coder.model.Spannable)
	 */
	public boolean before(Spannable s) {
		return getEndPosition() <= s.getStartPosition();
	}

	/* (non-Javadoc)
	 * @see edu.pitt.dbmi.nlp.noble.coder.model.Spannable#after(edu.pitt.dbmi.nlp.noble.coder.model.Spannable)
	 */
	public boolean after(Spannable s) {
		return s.getEndPosition() <= getStartPosition();
	}

	/**
	 * compare to other mentions.
	 *
	 * @param o the o
	 * @return the int
	 */
	public int compareTo(Mention o) {
		int n = getStartPosition() - o.getStartPosition();
		if(n == 0){
			n = getEndPosition() - o.getEndPosition();
			// if still equal, just arbitrary assign something
			// this is done, so that mentions would not be dropped in
			// some sorted set, since this method is often used to compare them
			//if(n == 0)
			//	n = -1;
		}
		return n;
	}
	
	/**
	 * convert a found concept to a set of mentions.
	 *
	 * @param c the c
	 * @return the mentions
	 */
	public static List<Mention> getMentions(Concept c) {
		return getMentions(c,Arrays.asList(c.getAnnotations()));
	}
	
	/**
	 * convert a found concept to a set of mentions.
	 *
	 * @param c the c
	 * @param annotations the annotations
	 * @return the mentions
	 */
	public static List<Mention> getMentions(Concept c, List<Annotation> annotations) {
		List<Mention> list = new ArrayList<Mention>();
		
		// if we could not get annotations, is it really worth it getting this concept?
		if(annotations.isEmpty())
			return Collections.EMPTY_LIST;
		
		
		// lets make a short cut, if we have the same number of annotations as words in match term, then we are good
		if(!(c.getMatchedTerms() != null && c.getMatchedTerms().length == 1 && annotations.size() == TextTools.getWords(c.getMatchedTerm()).size())){
			List<String> words = TextTools.getWords(c.getSearchString());
			// go over every word in a sentence
			for(String term: c.getMatchedTerms()){
				List<String> twords  = TextTools.getWords(term);
				int offs = 0;
				for(int i=0;i<words.size();i++){
					// if term words contain that word, then
					// look at the sublist that includes it and + allowed gap
					// if this sublist contains ALL term words, then we have contigous match
					// FROM MELISSA: the word window span is actually good, just need to do gap analysis after to 
					// make sure that no gap exceeds the word gap 
					
					if(twords.contains(words.get(i)) && c.getTerminology() != null && c.getTerminology() instanceof NobleCoderTerminology){
						int n = i+((((NobleCoderTerminology)c.getTerminology()).getMaximumWordGap()+1)*(twords.size()-1))+1;
						if(n >= words.size())
							n = words.size()-1;
						if(words.subList(i,n).containsAll(twords)){
							int st = c.getSearchString().indexOf(words.get(i),offs);
							int en = c.getSearchString().indexOf(words.get(n),offs);
							
							List<Annotation> alist = new ArrayList<Annotation>();
							for(Annotation a: annotations){
								if(st <= a.getStartPosition() && a.getEndPosition() <= en){
									alist.add(a);
								}
							}
							
							// create a mention for a contigus span of text
							if(!alist.isEmpty()){
								Mention m = new Mention();
								m.setConcept(c);
								m.setAnnotations(alist);
								list.add(m);
							}
						}
					}
					
					// keep track of offset
					offs += words.get(i).length()+1;
				}
			}
		}
		
		// if our prior step fail, do default action and simply add all annotation to a single mention
		if(list.isEmpty()){
			Mention m = new Mention();
			m.setConcept(c);
			m.setAnnotations(annotations);
			list.add(m);
		}
		
		return list;
	}


	/**
	 * modifier types used throught the system.
	 *
	 * @return the modifier types
	 */
	public static List<String> getModifierTypes(){
		return ConText.MODIFIER_TYPES;
	}

	/**
	 * get a mapping of linguistic context found for this mention.
	 *
	 * @return the modifiers
	 */
	public Map<String,Modifier> getModifiers(){
		if(modifiers == null){
			modifiers = new LinkedHashMap<String,Modifier>();
		}
		return modifiers;
	}
	
	/**
	 * Gets the modifier annotations.
	 *
	 * @return the modifier annotations
	 */
	public List<Annotation> getModifierAnnotations(){
		List<Annotation> list = new ArrayList<Annotation>();
		for(Modifier m: getModifiers().values()){
			list.addAll(m.getAnnotations());
		}
		return list;
	}

	/**
	 * add linguistic mofifier of this mention.
	 *
	 * @param m the m
	 */
	public void addModifier(Modifier m) {
		boolean add = false;
		if(getModifiers().containsKey(m.getType())){
			Modifier oldM = getModifiers().get(m.getType());
			// replace default modifier, with non default modifier
			if(oldM.isDefaultValue() && !m.isDefaultValue()){
				add = true;
			}
			// if new modifier is longer, then replace the old one
			if(m.getMention() != null && oldM.getMention() != null && m.getMention().contains(oldM.getMention())){
				add = true;
			}
		}else{
			// if no modifier of that type was defined 
			add = true;
		}
		
		// add modifier
		if(add)
			getModifiers().put(m.getType(),m);
		
		// if we don't have modifier defined for that type, or it is default, or the new value is not default
		//if(!getModifiers().containsKey(m.getType()) || getModifiers().get(m.getType()).isDefaultValue() || !m.isDefaultValue()){
		//	getModifiers().put(m.getType(),m);
		//}
	}
	
	/**
	 * add linguistic mofifier of this mention.
	 *
	 * @param list the list
	 */
	public void addModifiers(List<Modifier> list) {
		for(Modifier m: list){
			addModifier(m);
		}
	}
	
	/**
	 * Gets the modifier.
	 *
	 * @param type the type
	 * @return the modifier
	 */
	public Modifier getModifier(String type){
		return getModifiers().get(type);
	}
	
	/**
	 * Gets the modifier value.
	 *
	 * @param type the type
	 * @return the modifier value
	 */
	public String getModifierValue(String type){
		return getModifiers().containsKey(type)?getModifiers().get(type).getValue():null;
	}
	
	/**
	 * Checks if is negated.
	 *
	 * @return true, if is negated
	 */
	public boolean isNegated(){
		return ConText.MODIFIER_VALUE_NEGATIVE.equals(getModifierValue(ConText.MODIFIER_TYPE_POLARITY));
	}
	
	/**
	 * Checks if is hedged.
	 *
	 * @return true, if is hedged
	 */
	public boolean isHedged(){
		return ConText.MODIFIER_VALUE_HEDGED.equals(getModifierValue(ConText.MODIFIER_TYPE_MODALITY));
	}
	
	/**
	 * Checks if is historical.
	 *
	 * @return true, if is historical
	 */
	public boolean isHistorical(){
		return  ConText.MODIFIER_VALUE_HISTORICAL.equals(getModifierValue(ConText.MODIFIER_TYPE_TEMPORALITY));
	}
	
	/**
	 * Checks if is family member.
	 *
	 * @return true, if is family member
	 */
	public boolean isFamilyMember(){
		return ConText.MODIFIER_VALUE_FAMILY_MEMBER.equals(getModifierValue(ConText.MODIFIER_TYPE_EXPERIENCER));
	}
	

}
