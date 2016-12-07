package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.ArrayList;
import java.util.List;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;

public class Anchor {
	private DomainOntology domainOntology;
	private List<Mention> mentions;
	private IClass cls;
	
	
	public Anchor(DomainOntology ontology,Mention m){
		this.domainOntology = ontology;
		cls = ontology.getConceptClass(m);
		getMentions().add(m);
	}
	public DomainOntology getDomainOntology() {
		return domainOntology;
	}

	public void setDomainOntology(DomainOntology domainOntology) {
		this.domainOntology = domainOntology;
	}

	
	/**
	 * get a list of mentions associated with this anchor
	 * @return
	 */
	public List<Mention> getMentions(){
		if(mentions == null){
			mentions = new ArrayList<Mention>();
		}
		return mentions;
	}

	public IClass getConceptClass() {
		return cls;
	}
}
