package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
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
	protected Set<Annotation> annotations;
	
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
	
	/**
	 * initilize an instance
	 * @param ontology
	 * @param m
	 */
	
	public Instance(DomainOntology ontology,Mention m, IInstance inst){
		setDomainOntology(ontology);
		setMention(m);
		instance = inst;
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
		reset();
	}

	
	
	public Modifier getModifier() {
		return modifier;
	}

	public void setModifier(Modifier modifier) {
		this.modifier = modifier;
		setMention(modifier.getMention());
		if(mention == null)
			cls = domainOntology.getOntology().getClass(modifier.getValue());
		reset();
	}
	
	/**
	 * reset instance information
	 */
	protected void reset(){
		instance = null;
		modifierInstances = null;
		annotations = null;
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
				
				// if instance is modifier, but not linguistic modifier (see if we neet to set some other properties
				if(domainOntology.isTypeOf(cls,DomainOntology.MODIFIER) && !domainOntology.isTypeOf(cls,DomainOntology.LINGUISTIC_MODIFER)){
					// instantiate available modifiers
					List<Instance> modifierInstances = getModifierInstanceList();
					
					// go over all restrictions
					for(IRestriction r: domainOntology.getRestrictions(cls)){
						IProperty prop = r.getProperty();
						for(Instance modifierInstance: modifierInstances){
							IInstance modInstance = modifierInstance.getInstance();
							if(modInstance != null && domainOntology.isPropertyRangeSatisfied(prop,modInstance)){
								instance.addPropertyValue(prop, modInstance);
								addModifierInstance(prop.getName(),modifierInstance);
							}
						}
					}
				}
			}else if(modifier != null){
				instance = domainOntology.getOntology().getInstance(cls.getName()+"_default");
				if(instance == null)
					instance = cls.createInstance(cls.getName()+"_default");
				
			}
		}
		return instance;
	}
	
	public String getName(){
		return getConceptClass().getName();
	}
	public String getLabel(){
		return getConceptClass().getLabel();
	}
	
	
	/**
	 * pretty print this name
	 */
	public String toString(){
		StringBuffer str = new StringBuffer();
		str.append(getLabel());
		for(String type: getModifierInstances().keySet()){
			for(Instance modifier:getModifierInstances().get(type)){
				str.append(" "+type+": "+modifier);
			}
		}
		return str.toString();
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
	
	public Set<Instance> getModifierInstances(String prop){
		return getModifierInstances().get(prop);
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
	 * get a list of current modifiers as instance list
	 * @return
	 */
	public List<Instance> getModifierInstanceList(){
		// instantiate available modifiers
		List<Instance> modifierInstances = new ArrayList<Instance>();
		for(Modifier m: getModifiers()){
			modifierInstances.add(new Instance(domainOntology, m));
		}
		return modifierInstances;
	}
	
	/**
	 * add linguistic mofifier of this mention.
	 *
	 * @param m the m
	 */
	public void addModifier(Modifier m) {
		getModifiers().add(m);
		reset();
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


	public Set<Annotation> getAnnotations() {
		if(annotations == null){
			annotations = new TreeSet<Annotation>();
			if(getMention() != null)
				annotations.addAll(getMention().getAnnotations());
			for(String type: getModifierInstances().keySet()){
				for(Instance modifier:getModifierInstances().get(type)){
					annotations.addAll(modifier.getAnnotations());
				}
			}
		}
		
		return annotations;
	}
	
}
