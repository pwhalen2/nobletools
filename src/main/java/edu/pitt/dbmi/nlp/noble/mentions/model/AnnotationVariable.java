package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;

/**
 * This class represents a container for Annotation class inside DomainOntology.owl
 * @author tseytlin
 */
public class AnnotationVariable extends Instance {
	private Instance anchor;
	private String annotationType;
	//private List<Mention> mentions;
	
	/**
	 * create a new annotation variable wih anchor
	 * @param anchor
	 */
	public AnnotationVariable(IClass annotation,Instance anchor) {
		super(anchor.getDomainOntology(),anchor.getMention());
		this.anchor = anchor;
		this.cls = annotation;
		annotationType = DomainOntology.ANNOTATION_MENTION;
	}

	
	public Instance getAnchor() {
		return anchor;
	}
	
	/**
	 * get all of mentions associated with this variable
	 * @return
	 *
	public List<Mention> getMentions() {
		if(mentions == null){
			mentions = new ArrayList<Mention>();
			mentions.addAll(anchor.getMentions());
			for(Modifier m: getModifierList()){
				if(m.getMention() != null)
					mentions.add(m.getMention());
			}
		}
		return mentions;
	}
	*/

	

	public String getAnnotationType() {
		return annotationType;
	}


	public void setAnnotationType(String annotationType) {
		this.annotationType = annotationType;
	}

	
	/**
	 * get or create annotation type instance
	 * @return
	 */
	private IInstance getAnnotationTypeInstance(){
		IOntology ont = domainOntology.getOntology();
		IClass cls = ont.getClass(getAnnotationType());
		IInstance inst = ont.getInstance(cls.getName()+"Instance");
		if(inst == null)
			inst = cls.createInstance(cls.getName()+"Instance");
		return inst;
	}

	/**
	 * get an instance that represents this annotation variable
	 * @return
	 */
	public IInstance getInstance(){
		if(instance == null){
			// create an instance
			IOntology ont = cls.getOntology();
			instance = cls.createInstance(domainOntology.createInstanceName(cls));
			
			// add anchor
			instance.addPropertyValue(ont.getProperty(DomainOntology.HAS_ANCHOR),anchor.getInstance());
			addModifierInstance(DomainOntology.HAS_ANCHOR,anchor);
			
			// add type
			instance.addPropertyValue(ont.getProperty(DomainOntology.HAS_ANNOTATION_TYPE),getAnnotationTypeInstance());
			addModifierInstance(DomainOntology.HAS_ANNOTATION_TYPE,new Instance(domainOntology,Modifier.getModifier(DomainOntology.HAS_ANNOTATION_TYPE,getAnnotationType())));
			
			
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
		return instance;
	}
	
	
	
	
	/**
	 * is the current annotation variable satisfied given its linguistic and semantic properties?
	 * @return
	 */
	public boolean isSatisfied() {
		return getConceptClass().getEquivalentRestrictions().evaluate(getInstance());
	}

	public String toString(){
		StringBuffer str = new StringBuffer();
		str.append(getConceptClass()+"\n");
		for(String type: getModifierInstances().keySet()){
			for(Instance modifier:getModifierInstances().get(type)){
				str.append("\t"+type+": "+modifier+"\n");
			}
		}
		str.append("\thasText: "+getAnnotations());
		return str.toString();
	}
}
