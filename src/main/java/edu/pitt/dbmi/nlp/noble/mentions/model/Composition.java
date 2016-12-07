package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.*;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;

public class Composition extends Document {
	private List<AnnotationVariable> annotationVariables;
	
	public Composition(String text){
		super(text);
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
