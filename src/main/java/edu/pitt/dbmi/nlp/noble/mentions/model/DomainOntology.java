package edu.pitt.dbmi.nlp.noble.mentions.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.ontology.ClassPath;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.LogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.OntologyUtils;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
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
	public static final String COMPOUND_ANCHOR = "CompoundAnchor";
	public static final String MODIFIER = "Modifier";
	public static final String ANNOTATION = "Annotation";
	public static final List<String> ANCHOR_ROOTS =  Arrays.asList(ANCHOR,COMPOUND_ANCHOR);
	public static final List<String> MODIFIER_ROOTS =  Arrays.asList("Closure","Pseudo",MODIFIER);
	public static final String LANGUAGE = "en";
	public static final String SEMTYPE_INSTANCE = "Instance";
	public static final String HAS_ANCHOR = "hasAnchor";
	public static final String SCHEMA_OWL = "Schema.owl";
	public static final String HAS_MODIFIER = "hasModifier";
	public static final String IS_ANCHOR_OF = "isAnchorOf";
	public static final String HAS_ANNOTATION_TYPE = "hasAnnotationType";
	public static final String ANNOTATION_MENTION = "MentionAnnotation";
	public static final String LINGUISTIC_MODIFER = ConText.LINGUISTIC_MODIFIER;
	private static final String HAS_COMPOUND_ARGUMENT = "hasCompoundArgument";
	public static final String COMPOSITION = "Composition";
	public static final String HAS_TITLE = "hasTitle";
	public static final String HAS_MENTION_ANNOTATION = "hasMentionAnnotation";
	
	private IOntology ontology;
	private Terminology anchorTerminology, modifierTerminology;
	//private Map<String,SemanticType> semanticTypeMap;
	private ConText.ModifierValidator modifierValidator;
	private Map<IClass,Set<IClass>> compoundAnchorMap;
	
	/**
	 * File or URL location of the domain ontology
	 * @param location of ontology
	 * @throws IOntologyException  if there was something wrong
	 */
	public DomainOntology(String location) throws IOntologyException{
		//	this(OOntology.loadOntology(location));
		File file = new File(location);
		if(file.exists()){
			String ontologyURI = null;
			try {
				ontologyURI = ""+OntologyUtils.getOntologyURI(file);
			} catch (IOException e) {
				throw new IOntologyException("Unable get parent ontology URL "+location);
			}
			if(ontologyURI.endsWith(".owl"))
				ontologyURI = ontologyURI.substring(0,ontologyURI.length()-4);
			ontologyURI += "Instances.owl";
			setOntology(OOntology.createOntology(URI.create(ontologyURI),file));
		}else if (location.startsWith("http")){
			String ontologyURI = location;
			if(ontologyURI.endsWith(".owl"))
				ontologyURI = ontologyURI.substring(0,ontologyURI.length()-4);
			ontologyURI += "Instances.owl";
			setOntology(OOntology.createOntology(URI.create(ontologyURI),URI.create(location)));
		}else{
			throw new IOntologyException("Unable to identify ontology schema location "+location);
		}
		
	}
	
	/**
	 * File or URL location of the domain ontology
	 * @param ont - ontology that this domain is based on
	 * @throws IOntologyException if something went wrong
	 */
	public DomainOntology(IOntology ont) throws IOntologyException{
		setOntology(ont); 
	}
	
	
	/**
	 * get ontology object
	 * @return IOntology object
	 */
	public IOntology getOntology() {
		return ontology;
	}


	/**
	 * set domain ontology based on http://blulab.chpc.utah.edu/ontologies/v2/Schema.owl
	 * @param ontology - an ontology object representing domain ontology
	 * @throws IOntologyException  in case something went wrong
	 */

	public void setOntology(IOntology ontology) throws IOntologyException {
		this.ontology = ontology;
		if(anchorTerminology != null)
			anchorTerminology.dispose();
		if(modifierTerminology != null)
			modifierTerminology.dispose();
		anchorTerminology = null;
		modifierTerminology = null;
	}

	
	/**
	 * check if ontology is complient with Schema.owl ontology
	 * @return true or false
	 */
	public boolean isOntologyValid(){
		// make sure it derives from schema
		boolean fromSchema = false;
		for(IOntology ont: getOntology().getImportedOntologies()){
			if(ont.getURI().toString().contains(SCHEMA_OWL)){
				fromSchema = true;
				break;
			}
		}
		/*if(!fromSchema){
			throw new IOntologyException("Ontology "+ontology.getName()+" does not derive from "+SCHEMA_OWL+" ontology");
		}*/
		return fromSchema;
	}


	/**
	 * get a terminology of anchors  
	 * @return anchor terminology
	 */
	public Terminology getAnchorTerminology() {
		if(anchorTerminology == null){
			NobleCoderTerminology terminology = new NobleCoderTerminology();
			terminology.setName("AnchorTerminology");
			//TODO: maybe custom params
			// set language filter to only return English values
			if(ontology instanceof OOntology)
				((OOntology)ontology).setLanguageFilter(Arrays.asList(LANGUAGE));
			
			for(String root: ANCHOR_ROOTS){
				IClass cls = ontology.getClass(root);
				if(cls != null){
					// add roots to terminology
					terminology.addRoot(addConcept(terminology,cls).getCode());
					// go over all subclasses
					for(IClass c: cls.getSubClasses()){
						addConcept(terminology,c);
					}
				}
			}
			//semanticTypeMap = null;
			anchorTerminology = terminology;
		}
		
		return anchorTerminology;
	}

	

	/**
	 * create a modifier terminology for a given domain ontology
	 * @return modifier terminology
	 */
	public Terminology getModifierTerminology() {
		if(modifierTerminology == null){
			// setup special interest of noble coder
			NobleCoderTerminology terminology = new NobleCoderTerminology();
			//terminology.load(getClass().getSimpleName(),false);
			terminology.setName("ModifierTerminology");
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
	 * get anchor and modifier terminology together
	 * @return all terminologies associated with this ontology
	 */
	public Terminology [] getTerminologies(){
		return new Terminology [] {getAnchorTerminology(),getModifierTerminology()};
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
			
			// add inverse relationships based on ranges
			addInverseRelationships(inst.getDirectTypes()[0],ontology.getProperty(HAS_MODIFIER), concept);
			
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
			
			// add inverse relationships based on ranges
			addInverseRelationships(cls,ontology.getProperty(HAS_MODIFIER), concept);
			
			terminology.addConcept(concept);
			
			return concept;
		}catch(TerminologyException ex){
			throw new TerminologyError("Unable to add a concept object",ex);
		}
	}
	
	/**
	 * add inverse relationships for a class from top property to a concept
	 * @param cls that has relationship
	 * @param hasModifier -property
	 * @param concept - concept to add the inverse relationship to
	 */
	private void addInverseRelationships(IClass cls, IProperty hasModifier, Concept concept){
		for(IProperty prop: hasModifier.getSubProperties()){
			LogicExpression exp = new LogicExpression(ILogicExpression.OR,prop.getRange());
			if(exp.evaluate(cls)){
				for(IClass domain: prop.getDomain()){
					concept.addRelatedConcept(Relation.getRelation(getInversePropertyName(prop.getName())), domain.getName());	
				}
			}
		}
	}
	
	
	/**
	 * convert the property name that follows a certain convention to a likely inverted form
	 * Ex:  hasModifier -> isModifierOf and vice versa
	 * @param name - original property name
	 * @return inverse property name
	 */
	private String getInversePropertyName(String name){
		if(name.startsWith("has")){
			return "is"+name.substring(3)+"Of";
		}else if(name.startsWith("is") && name.endsWith("Of")){
			return "has"+name.substring(2,name.length()-2);
		}
		return name;
	}
	
	
	/**
	 * get concept class for a given mention
	 * @param mention object
	 * @return class that represents this mention
	 */
	public IClass getConceptClass(Mention mention){
		if(mention == null)
			return null;
		return getConceptClass(mention.getConcept());
	}
	
	/**
	 * get concept class for a given concept
	 * @param concept object
	 * @return class object
	 */
	public IClass getConceptClass(Concept concept) {
		if(concept == null)
			return null;
		String uri = concept.getCode(Source.URI);
		if(uri != null){
			IClass cls = ontology.getClass(uri);
			if(cls == null){
				IInstance inst = ontology.getInstance(uri);
				if(inst != null){
					return inst.getDirectTypes()[0];
				}
			}
			return cls;
		}
		return null;
	}

	/**
	 * get concept class for a given mention
	 * @param mention object
	 * @return ontology instnace object
	 */
	public IInstance getConceptInstance(Mention mention){
		if(mention == null)
			return null;
		return getConceptInstance(mention.getConcept());
	}
	
	/**
	 * get concept class for a given concept
	 * @param concept object
	 * @return ontology instance object
	 */
	public IInstance getConceptInstance(Concept concept) {
		if(concept == null)
			return null;
		String uri = concept.getCode(Source.URI);
		if(uri != null){
			return ontology.getInstance(uri);
		}
		return null;
	}
	
	
	/**
	 * get a list of anchors for given list of mentions typically in a sentence 
	 * Anchors can be compound anchors too.
	 * @param mentions - list of them
	 * @return list of instances
	 */
	public List<Instance> getAnchors(List<Mention> mentions) {
		List<Instance> anchors = new ArrayList<Instance>();

		// go all mentions and create anchors for them
		for(Mention m: mentions){
			if(isAnchor(m)){
				anchors.add(new Instance(this,m));
			}
		}
				
		// add compound anchors as well 
		for(Instance a: getCompoundAnchors(mentions)){
			anchors.add(a);
		}
		return anchors;
	}

	/**
	 * is mention an anchor?
	 * @param m - mention object in question
	 * @return true ro false
	 */
	private boolean isAnchor(Mention m) {
		IClass cls = getConceptClass(m);
		return cls != null ? cls.hasSuperClass(ontology.getClass(ANCHOR)):false;
	}
	
	/**
	 * is a mention of a given type
	 * @param m - mention object in question
	 * @param type - type in question
	 * @return true or not
	 */
	public boolean isTypeOf(Mention m, String type){
		return isTypeOf(getConceptClass(m), type);
	}
	
	/**
	 * is a mention of a given type
	 * @param cls - class in question
	 * @param type - type in question
	 * @return true or not
	 */
	public boolean isTypeOf(IClass cls, String type){
		return cls != null ? cls.hasSuperClass(ontology.getClass(type)):false;
	}
	
	/**
	 * get a list of compount anchors that can be constructed from a given set of mentions
	 * @param mentions list of them
	 * @return list of Instance objects
	 */
	private List<Instance> getCompoundAnchors(List<Mention> mentions) {
		List<Instance> compound = new ArrayList<Instance>();
		
		// fill in mention map
		//TODO: what if several mentions with same class?
		final Map<IClass,Mention> mentionMap = new LinkedHashMap<IClass, Mention>();
		for(Mention m: mentions){
			if(isAnchor(m))
				mentionMap.put(getConceptClass(m),m);
		}
		
		// skip if nothing to do
		if(mentionMap.isEmpty() || getCompoundAnchorMap().isEmpty())
			return Collections.EMPTY_LIST;
		
		
		// get property
		IProperty hasCompoundArgument = ontology.getProperty(HAS_COMPOUND_ARGUMENT);
		Set<IClass> foundCompounds = new HashSet<IClass>();
		
		
		boolean change = false;
		do{
			// resort the mentions based on their position in text
			Set<IClass> mentionedClasses = new TreeSet<IClass>(new Comparator<IClass>() {
				public int compare(IClass o1, IClass o2) {
					return mentionMap.get(o1).compareTo(mentionMap.get(o2));
				}
			});
			mentionedClasses.addAll(mentionMap.keySet());
			
			// go over all compounds anchors
			change = false;
			for(IClass compoundCls: getCompoundAnchorMap().keySet()){
				// skip classes that were already found
				if(foundCompounds.contains(compoundCls))
					continue;
				
				
				// find classes that are possible arguments
				Set<IClass> possibleArgs = getPossibleCompoundAnchorArguments(compoundCls,mentionedClasses);
				IRestriction [] compoundRestrictions = compoundCls.getRestrictions(hasCompoundArgument);
				
				// if number of possible arguments is good, try to see if we can match it for real
				if(possibleArgs.size() >= compoundRestrictions.length && compoundRestrictions.length > 0){
					// create an instance and see if it is satisfiable
					IInstance inst = compoundCls.createInstance(createInstanceName(compoundCls));
					List<IInstance> componentInst = new ArrayList<IInstance>();
					List<Mention> possibleMentionComponents = new ArrayList<Mention>();
					int n = 1;
					String hasCompoundPrefix = hasCompoundArgument.getName();
					for(IClass c: possibleArgs){
						IInstance i = c.createInstance(createInstanceName(c));
						IProperty argProperty  = (n <= 5)?ontology.getProperty(hasCompoundPrefix+(n++)):hasCompoundArgument;
						inst.addPropertyValue(argProperty,i);
						componentInst.add(i);
						possibleMentionComponents.add(mentionMap.get(c));
					}
					
					// moment of truth does it work????
					if(compoundCls.getEquivalentRestrictions().evaluate(inst)){
						Mention mention = createCompoundAnchorMention(compoundCls, possibleMentionComponents);
						mentionMap.put(compoundCls,mention);
						compound.add( new Instance(this,mention,inst));
						foundCompounds.add(compoundCls);
						change = true;
					}else{
						//clean up
						for(IInstance i: componentInst){
							i.delete();
						}
						inst.delete();
					}
					
				}
			}
		}while(change);
		
		return compound;
	}
	
	/**
	 * create compound anchor mentions
	 * @param compoundCls - compund class
	 * @param components - list of mention objects that are its parts
	 * @return combined Mention object
	 */
	public Mention createCompoundAnchorMention(IClass compoundCls, Collection<Mention> components){
		Concept concept = null;
		try {
			concept = getAnchorTerminology().lookupConcept(compoundCls.getName());
		} catch (TerminologyException e) {
			throw new TerminologyError("Could not find concept "+compoundCls.getName(),e);
		}
		// create new mention
		Mention mention = new Mention();
		mention.setConcept(concept);
		List<Annotation> annotations = new ArrayList<Annotation>();
		for(Mention m: components){
			if(mention.getSentence() == null)
				mention.setSentence(m.getSentence());
			annotations.addAll(m.getAnnotations());
			mention.getModifiers().putAll(m.getModifiers());
		}
		mention.setAnnotations(annotations);
		
		return mention;
	}
	
	
	/**
	 * get possible compoung anchor arguments
	 * 
	 * @param compoundCls - compound class in question
	 * @param mentionsClss -  mention classes
	 * @return set of classes that are arguments
	 */
	
	private Set<IClass> getPossibleCompoundAnchorArguments(IClass compoundCls,Set<IClass> mentionsClss){
		Set<IClass> found = new LinkedHashSet<IClass>();
		/*for(IClass component: getCompoundAnchorMap().get(compoundCls)){
			if(mentionsClss.contains(component))
				found.add(component);
		}*/
		for(IClass mention: mentionsClss){
			if(getCompoundAnchorMap().get(compoundCls).contains(mention))
				found.add(mention);
		}
		
		return found;
	}
	
	
	
	/**
	 * get the mapping between compound anchors and its components
	 * @return compound anchor map
	 */
	private Map<IClass,Set<IClass>> getCompoundAnchorMap(){
		if(compoundAnchorMap == null){
			compoundAnchorMap = new HashMap<IClass, Set<IClass>>();
			for(IClass cls: ontology.getClass(COMPOUND_ANCHOR).getSubClasses()){
				// get all possible component classes
				Set<IClass> possibleComponents = new LinkedHashSet<IClass>(); 
				for(IRestriction r: cls.getRestrictions(ontology.getProperty(HAS_COMPOUND_ARGUMENT))){
					possibleComponents.addAll(getContainedClasses(r.getParameter()));
				}
				// 
				compoundAnchorMap.put(cls,possibleComponents);
			}
		}
		return compoundAnchorMap;
	}
	
	
	
	/**
	 * get all classes contained in a given expression
	 * @param exp - logical expression
	 * @return list of classes
	 */
	public List<IClass> getContainedClasses(ILogicExpression exp){
		List<IClass> classes = new ArrayList<IClass>();
		for(Object o: exp){
			if(o instanceof IClass){
				classes.add((IClass)o);
			}else if(o instanceof ILogicExpression){
				classes.addAll(getContainedClasses((ILogicExpression)o));
			}
		}
		return classes;
	}
	/**
	 * get all classes contained in a given expression
	 * @param rr set of restrictions
	 * @return  list of classes
	 */
	public List<IClass> getContainedClasses(IRestriction [] rr){
		List<IClass> classes = new ArrayList<IClass>();
		for(IRestriction r: rr){
			classes.addAll(getContainedClasses(r.getParameter()));
		}
		return classes;
	}
	
	
	
	/**
	 * get modifier target validator to check if modifier can be attached to target
	 * @return modifier validator
	 */
	public ConText.ModifierValidator getModifierValidator(){
		if(modifierValidator == null){
			modifierValidator = new ConText.ModifierValidator() {
				public boolean isModifierApplicable(Mention modifier, Mention target) {
					// get an annotation class for this target
					if(target.getConcept().getRelationMap().containsKey(IS_ANCHOR_OF)){
						for(String annotoationName: target.getConcept().getRelationMap().get(IS_ANCHOR_OF)){
							IClass annotationCls = ontology.getClass(annotoationName);
							if(annotationCls != null){
								for(IRestriction r: getRestrictions(annotationCls)){
									String inverseProp = getInversePropertyName(r.getProperty().getName());
									// if we got an inverse property, awesome lets look if they match
									if(modifier.getConcept().getRelationMap().containsKey(inverseProp)){
										for(String domainName: modifier.getConcept().getRelationMap().get(inverseProp)){
											IClass domainCls = ontology.getClass(domainName);
											if(domainCls != null && domainCls.evaluate(annotationCls)){
												return true;
											}
										}
									}
								}
							}
						}
					}
					// check if the target explicitly defines a relationship
					IClass modifierCls = getConceptClass(modifier);
					for(String propName: target.getConcept().getRelationMap().keySet()){
						IProperty prop = ontology.getProperty(propName);
						if(prop != null && isPropertyRangeSatisfied(prop, modifierCls)){
							return true;
						}
					}
					return false;
				}
			};
		}
		return modifierValidator;
	}
	
	
	/**
	 * is property range satisfied with a given class?
	 * @param prop - property in question
	 * @param cls - class in question
	 * @return true or false
	 */
	public boolean isPropertyRangeSatisfied(IProperty prop, IClass cls){
		if(cls == null)
			return false;
		LogicExpression exp = new LogicExpression(ILogicExpression.OR,prop.getRange());
		return exp.evaluate(cls);
	}
	
	/**
	 * is property range satisfied with a given class?
	 * @param prop- property in question
	 * @param inst - instance in question
	 * @return true or false
	 */
	public boolean isPropertyRangeSatisfied(IProperty prop, IInstance inst){
		if(inst == null)
			return false;
		LogicExpression exp = new LogicExpression(ILogicExpression.OR,prop.getRange());
		return exp.evaluate(inst);
	}
	
	/**
	 * get all restrictions equivalent and necessary as a flat list
	 * @param cls - class in question
	 * @return get all restrictions for a class
	 */
	public List<IRestriction> getRestrictions(IClass cls){
		List<IRestriction> list = new ArrayList<IRestriction>();
		for(ILogicExpression exp: Arrays.asList(cls.getEquivalentRestrictions(),cls.getNecessaryRestrictions())){
			for(Object obj: exp){
				if(obj instanceof IRestriction){
					list.add((IRestriction)obj);
				}
			}
		}
		return list;
	}
	
	
	/**
	 * get a list of annotation variables that can be associated with a given anchor
	 * @param anchor - that may be related to variables
	 * @return list of annotation variables
	 */
	public List<AnnotationVariable> getAnnotationVariables(Instance anchor){
		List<AnnotationVariable> list = new ArrayList<AnnotationVariable>();
		// go over all annotation classes and find ones that have this anchor defined
		/*
		IProperty hasAnchor = ontology.getProperty(HAS_ANCHOR);
		IClass cls = anchor.getConceptClass();
		for(IClass annotation: ontology.getClass(ANNOTATION).getSubClasses()){
			if(isDefinedInDomain(annotation) && hasDefinedRelation(annotation, hasAnchor, cls)){
				list.add(new AnnotationVariable(annotation,anchor));
			}
		}
		*/
		// find annotations that anchor points to
		IProperty isAnchorOf = ontology.getProperty(IS_ANCHOR_OF);
		for(IClass annotation: getContainedClasses(anchor.getConceptClass().getRestrictions(isAnchorOf))){
			list.add(new AnnotationVariable(annotation,anchor));
		}
		return list;
	}

	/**
	 * does a given class relate to another class via a property
	 * @param cls  - a class that we are testing
	 * @param prop - a property that this class is related to another class
	 * @param comp - a component class
	 * @return true or false
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
	 * create usable and unique instance name
	 * @param cls in question
	 * @return  unique instnace name
	 */
	public String createInstanceName(IClass cls){
		return cls.getName()+"-"+System.currentTimeMillis()+"-"+((int)(Math.random()*1000));
	}
	
	/**
	 * ontology name
	 * @return name of ontology
	 */
	public String toString(){
		return ontology.getName();
	}
	
	
	/**
	 * output file to write the ontology as
	 * @param outputFile - that we want to save to
	 * @throws IOntologyException 
	 * @throws FileNotFoundException 
	 */
	public void write(File outputFile) throws FileNotFoundException, IOntologyException{
		ontology.write(new FileOutputStream(outputFile),IOntology.OWL_FORMAT);
	}

	/**
	 * ontology name
	 * @return  name of ontology
	 */
	public String getName() {
		return ontology.getName();
	}
	
}
