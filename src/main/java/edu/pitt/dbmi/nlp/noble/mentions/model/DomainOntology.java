package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyError;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.ConText;

/**
 * This class is a wrapper for http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl
 * DomainOntology.owl was developed by Melissa Castine and Wendy Chapmen (University of Utah)
 * @author Eugene Tseytlin (University of Pittsburgh)
 */
public class DomainOntology {
	public static final String ANCHOR = "Anchor";
	public static final String ANNOTATION = "Annotation";
	public static final List<String> ANCHOR_ROOTS =  Arrays.asList(ANCHOR);
	public static final List<String> MODIFIER_ROOTS =  Arrays.asList("Closure","Pseudo","LinguisticModifier");
	public static final String LANGUAGE = "en";
	public static final String SEMTYPE_INSTANCE = "Instance";
	public static final String HAS_ANCHOR = "hasAnchor";

	
	
	
	private IOntology ontology;
	private Terminology anchorTerminology, modifierTerminology;
	
	/**
	 * File or URL location of the domain ontology
	 * @param location
	 * @throws IOntologyException 
	 */
	public DomainOntology(String location) throws IOntologyException{
		setOntology(OOntology.loadOntology(location)); 
	}
	
	public IOntology getOntology() {
		return ontology;
	}


	/**
	 * set domain ontology based on http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl
	 * @param ontology - an ontology object representing domain ontology
	 */

	public void setOntology(IOntology ontology) {
		this.ontology = ontology;
		if(anchorTerminology != null)
			anchorTerminology.dispose();
		if(modifierTerminology != null)
			modifierTerminology.dispose();
		anchorTerminology = null;
		modifierTerminology = null;
	}



	/**
	 * get a terminology of anchors  
	 * @return
	 */
	public Terminology getAnchorTerminology() {
		if(anchorTerminology == null){
			NobleCoderTerminology terminology = new NobleCoderTerminology();
			//TODO: maybe custom params
			// set language filter to only return English values
			if(ontology instanceof OOntology)
				((OOntology)ontology).setLanguageFilter(Arrays.asList(LANGUAGE));
			
			for(String root: ANCHOR_ROOTS){
				IClass cls = ontology.getClass(root);
				if(cls != null){
					// add roots to terminology
					//terminology.addRoot(addConcept(terminology,cls).getCode());
					// go over all subclasses
					for(IClass c: cls.getSubClasses()){
						addConcept(terminology,c);
					}
				}
			}
			anchorTerminology = terminology;
		}
		
		return anchorTerminology;
	}

	
	/**
	 * create a modifier terminology for a given domain ontology
	 * @return
	 */
	public Terminology getModifierTerminology() {
		if(modifierTerminology == null){
			// setup special interest of noble coder
			NobleCoderTerminology terminology = new NobleCoderTerminology();
			//terminology.load(getClass().getSimpleName(),false);
			terminology.setDefaultSearchMethod(NobleCoderTerminology.CUSTOM_MATCH);
			terminology.setContiguousMode(true);
			terminology.setSubsumptionMode(false);
			terminology.setOverlapMode(true);
			terminology.setPartialMode(false);
			terminology.setOrderedMode(true);
			terminology.setMaximumWordGap(0);
			terminology.setScoreConcepts(false);
			terminology.setHandlePossibleAcronyms(false);
			terminology.setLanguageFilter(new String [] {LANGUAGE});
			terminology.setStemWords(false);
			terminology.setStripStopWords(false);
			terminology.setIgnoreSmallWords(false);
			terminology.setIgnoreDigits(false);
			terminology.setSemanticTypeFilter(SEMTYPE_INSTANCE);
			
			// set language filter to only return English values
			if(ontology instanceof OOntology)
				((OOntology)ontology).setLanguageFilter(Arrays.asList(LANGUAGE));
			
			
			// load classes 
			//TODO: semantic modifiers and skip empty ones
			for(String root: MODIFIER_ROOTS ){
				IClass cls = ontology.getClass(root);
				if(cls != null){
					// add roots to terminology
					terminology.addRoot(addConcept(terminology,cls).getCode());
					for(IInstance inst : cls.getDirectInstances()){
						addConcept(terminology,inst);
					}
					
					// go over all subclasses
					for(IClass c: cls.getSubClasses()){
						addConcept(terminology,c);
						for(IInstance inst : c.getDirectInstances()){
							addConcept(terminology,inst);
						}
					}
				}
			}
			modifierTerminology = terminology;
			
		}
		return modifierTerminology;
	}

	/**
	 * Adds the concept.
	 *
	 * @param inst the inst
	 * @return the concept
	 * @throws TerminologyException the terminology exception
	 */
	private Concept addConcept(Terminology terminology, IInstance inst) {
		try{
			Concept concept = ConText.createConcept(inst);
			terminology.addConcept(concept);
			return concept;
		}catch(TerminologyException ex){
			throw new TerminologyError("Unable to add a concept object",ex);
		}
	}
	
	/**
	 * Adds the concept.
	 *
	 * @param cls the cls
	 * @return the concept
	 * @throws TerminologyException the terminology exception
	 */
	private Concept addConcept(Terminology terminology, IClass cls) {
		try{
			Concept concept =  ConText.createConcept(cls);
			terminology.addConcept(concept);
			return concept;
		}catch(TerminologyException ex){
			throw new TerminologyError("Unable to add a concept object",ex);
		}
	}
	
	/**
	 * get concept class for a given mention
	 * @param mention
	 * @return
	 */
	public IClass getConceptClass(Mention mention){
		return getConceptClass(mention.getConcept());
	}
	
	/**
	 * get concept class for a given concept
	 * @param concept
	 * @return
	 */
	public IClass getConceptClass(Concept concept) {
		String uri = concept.getCode(Source.URI);
		return uri != null?ontology.getClass(uri):null;
	}

	/**
	 * get a list of anchors for given list of mentions typically in a sentence 
	 * Anchors can be compound anchors too.
	 * @param mentions
	 * @return
	 */
	public List<Anchor> getAnchors(List<Mention> mentions) {
		List<Anchor> anchors = new ArrayList<Anchor>();

		// go all mentions and create anchors for them
		for(Mention m: mentions){
			if(isAnchor(m)){
				anchors.add(new Anchor(this,m));
			}
		}
				
		// add compound anchors as well 
		for(Anchor a: getCompoundAnchors(mentions)){
			anchors.add(a);
		}
		
		return anchors;
	}

	private boolean isAnchor(Mention m) {
		IClass cls = getConceptClass(m);
		return cls != null ? cls.hasSubClass(ontology.getClass(ANCHOR)):false;
	}

	/**
	 * get a list of compount anchors that can be constructed from a given set of mentions
	 * @param mentions
	 * @return
	 */
	private List<Anchor> getCompoundAnchors(List<Mention> mentions) {
		// TODO Auto-generated method stub
		return Collections.EMPTY_LIST;
	}
	
	
	/**
	 * get a list of annotation variables that can be associated with a given anchor
	 * @param anchor2
	 * @return
	 */
	public List<AnnotationVariable> getAnnotationVariables(Anchor anchor){
		List<AnnotationVariable> list = new ArrayList<AnnotationVariable>();
		// go over all annotation classes and find ones that have this anchor defined
		IProperty hasAnchor = ontology.getProperty(HAS_ANCHOR);
		IClass cls = anchor.getConceptClass();
		for(IClass annotation: ontology.getClass(ANNOTATION).getSubClasses()){
			if(isDefinedInDomain(annotation) && hasDefinedRelation(annotation, hasAnchor, cls)){
				AnnotationVariable var = new AnnotationVariable(anchor);
				//TODO: look for other semantic properties beyound the sentence?
				if(var.isSatisfied())
					list.add(var);
			}
		}
		return list;
	}

	/**
	 * does a given class relate to another class via a property
	 * @param cls  - a class that we are testing
	 * @param prop - a property that this class is related to another class
	 * @param comp - a component class
	 * @return
	 */
	private boolean hasDefinedRelation(IClass cls, IProperty prop, IClass comp){
		for(IRestriction r: cls.getRestrictions(prop)){
			if(r.getParameter().evaluate(comp)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * is class defined in domain ontology
	 * @param cls
	 * @return
	 */
	private boolean isDefinedInDomain(IClass cls) {
		return cls.getURI().toString().startsWith(ontology.getURI().toString());
	}
	
	
	
}
