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
	 * @param ontology of the domain
	 * @param m - mention object
	 */
	
	public Instance(DomainOntology ontology,Mention m){
		setDomainOntology(ontology);
		setMention(m);
	}
	
	/**
	 * initilize an instance
	 * @param ontology object
	 * @param m - modifier object
	 */
	
	public Instance(DomainOntology ontology,Modifier m){
		setDomainOntology(ontology);
		setModifier(m);
	}
	
	/**
	 * initilize an instance
	 * @param ontology - domain ontology
	 * @param m  - mention object
	 * @param inst - ontology instance
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

	/**
	 * set mention associated with this instnace
	 * @param mention object
	 */
	public void setMention(Mention mention) {
		this.mention = mention;
		cls = domainOntology.getConceptClass(mention);
		if(mention != null)
			getModifiers().addAll(mention.getModifiers().values());
		reset();
	}

	/**
	 * get modifier associated with this instance
	 * @return modifier object
	 */
	
	public Modifier getModifier() {
		return modifier;
	}
	
	/**
	 * set modifier object
	 * @param modifier object 
	 */
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
	
	/**
	 * get mention associated with this class
	 * @return mention object
	 */

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
	 * @return class that represents this instance
	 */
	
	public IClass getConceptClass() {
		return cls;
	}
	
	/**
	 * get an instance representing this mention
	 * @return ontology instance
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
	/**
	 * get name of this instance (derived from class name)
	 * @return name of this instance
	 */
	public String getName(){
		if(getConceptClass() != null)
			return getConceptClass().getName();
		return modifier != null?modifier.getValue():"unknown";
	}
	
	/**
	 * get human preferred label for this instance
	 * @return label of this instnace, returns name if label not available
	 */
	public String getLabel(){
		if(getConceptClass() != null)
			return getConceptClass().getLabel();
		return modifier != null?modifier.getValue():"unknown";
	}
	
	/**
	 * pretty print this name
	 * @return pretty printed version of instance
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
	 * @return the modifiers map
	 */
	public Map<String,Set<Instance>> getModifierInstances(){
		if(modifierInstances == null){
			modifierInstances = new LinkedHashMap<String,Set<Instance>>();
		}
		return modifierInstances;
	}
	
	/**
	 * get a set of instances associated via given property
	 * @param prop - property by which instances are mapped
	 * @return set of instances
	 */
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
	 * @return list of modifier instances
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
	 * @param property by which modifier is related
	 * @param inst - instance of modifier
	 */
	public void addModifierInstance(String property, Instance inst){
		Set<Instance> list = getModifierInstances().get(property);
		if(list == null){
			list = new LinkedHashSet<Instance>();
		}
		list.add(inst);
		getModifierInstances().put(property,list);
	}


	/**
	 * get a set of text annotations associated with this instance
	 * @return set of annotations
	 */
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
