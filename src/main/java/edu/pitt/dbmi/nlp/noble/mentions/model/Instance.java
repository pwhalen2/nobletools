package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;

/**
 * a domain instance is a wrapper for any instance and mention found in text 
 * @author tseytlin
 *
 */
public class Instance {
	protected DomainOntology domainOntology;
	protected Mention mention;
	protected Modifier modifier;
	protected IClass cls;
	protected IInstance instance;
	protected List<Modifier> modifiers;
	protected Map<String,Set<Instance>> modifierInstances;

	/**
	 * initilize an instance
	 * @param ontology
	 * @param m
	 */
	
	public Instance(DomainOntology ontology,Mention m){
		setDomainOntology(ontology);
		setMention(m);
	}
	
	/**
	 * initilize an instance
	 * @param ontology
	 * @param m
	 */
	
	public Instance(DomainOntology ontology,Modifier m){
		setDomainOntology(ontology);
		setModifier(m);
	}
	
	public DomainOntology getDomainOntology() {
		return domainOntology;
	}

	public void setDomainOntology(DomainOntology domainOntology) {
		this.domainOntology = domainOntology;
	}

	
	public void setMention(Mention mention) {
		this.mention = mention;
		cls = domainOntology.getConceptClass(mention);
		if(mention != null)
			getModifiers().addAll(mention.getModifiers().values());
	}

	
	
	public Modifier getModifier() {
		return modifier;
	}

	public void setModifier(Modifier modifier) {
		this.modifier = modifier;
		setMention(modifier.getMention());
		if(mention == null)
			cls = domainOntology.getOntology().getClass(modifier.getValue());
	}

	public Mention getMention() {
		return mention;
	}
	
	/**
	 * get a list of mentions associated with this anchor
	 * @return
	 *
	public List<Mention> getMentions(){
		if(mentions == null){
			mentions = new ArrayList<Mention>();
		}
		return mentions;
	}
	 */
	
	/**
	 * get concept class representing this mention
	 * @return
	 */
	
	public IClass getConceptClass() {
		return cls;
	}
	
	/**
	 * get an instance representing this mention
	 * @return
	 */
	public IInstance getInstance() {
		// what's the point if we have no class?
		if(cls == null)
			return null;
		
		// init instance
		if(instance == null){
			// check if we have an actual mention or some generic default value w/out a mention
			if(mention != null){
				instance = cls.createInstance(domainOntology.createInstanceName(cls));
			}else if(modifier != null){
				instance = domainOntology.getOntology().getInstance(cls.getName()+"_default");
				if(instance == null)
					instance = cls.createInstance(cls.getName()+"_default");
				
			}
		}
		return instance;
	}
	
	/**
	 * pretty print this name
	 */
	public String toString(){
		return getConceptClass().getName();
	}
	
	
	/**
	 * get a mapping of linguistic context found for this mention.
	 *
	 * @return the modifiers
	 */
	public Map<String,Set<Instance>> getModifierInstances(){
		if(modifierInstances == null){
			modifierInstances = new LinkedHashMap<String,Set<Instance>>();
		}
		return modifierInstances;
	}
	
	/**
	 * get a list of modifiers associated with this instance
	 * @return the modifiers
	 */
	public List<Modifier> getModifiers(){
		if(modifiers == null){
			modifiers = new ArrayList<Modifier>();
		}
		return modifiers;
	}
	
	/**
	 * add linguistic mofifier of this mention.
	 *
	 * @param m the m
	 */
	public void addModifier(Modifier m) {
		getModifiers().add(m);
		instance = null;
	}

	
	/**
	 * add modifier instance
	 * @param property
	 * @param inst
	 */
	public void addModifierInstance(String property, Instance inst){
		Set<Instance> list = getModifierInstances().get(property);
		if(list == null){
			list = new LinkedHashSet<Instance>();
		}
		list.add(inst);
		getModifierInstances().put(property,list);
	}


	public List<Annotation> getAnnotations() {
		return getMention().getAnnotations();
	}
	
}
