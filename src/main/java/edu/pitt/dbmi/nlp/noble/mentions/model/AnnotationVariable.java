package edu.pitt.dbmi.nlp.noble.mentions.model;


/**
 * This class represents a container for Annotation class inside DomainOntology.owl
 * @author tseytlin
 */
public class AnnotationVariable {
	private Anchor anchor;
	private DomainOntology domainOntology;
	
	/**
	 * create a new annotation variable wih anchor
	 * @param anchor
	 */
	public AnnotationVariable(Anchor anchor) {
		this.anchor = anchor;
		this.domainOntology = anchor.getDomainOntology();
	}

	
	/**
	 * is the current annotation variable satisfied given its linguistic and semantic properties?
	 * @return
	 */
	public boolean isSatisfied() {
		// TODO Auto-generated method stub
		return true;
	}

}
