package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.*;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;

public class Composition extends Document {
	private List<AnnotationVariable> annotationVariables;
	private DomainOntology domainOntology;
	
	public Composition(String text){
		super(text);
	}
	
	public DomainOntology getDomainOntology() {
		return domainOntology;
	}

	public void setDomainOntology(DomainOntology domainOntology) {
		this.domainOntology = domainOntology;
	}

	/**
	 * get a set of annotation variables extracted from a document
	 * @return
	 */
	public List<AnnotationVariable> getAnnotationVariables(){
		if(annotationVariables == null)
			annotationVariables = new ArrayList<AnnotationVariable>();
		return annotationVariables;
	}

	/**
	 * add an annotation variable
	 * @param var
	 */
	public void addAnnotationVariable(AnnotationVariable var) {
		getAnnotationVariables().add(var);
	}
}
