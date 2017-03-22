package edu.pitt.dbmi.nlp.noble.eval.gold;

import static edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology.ANNOTATION;
import static edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology.COMPOSITION;
import static edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology.HAS_MENTION_ANNOTATION;
import static edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology.HAS_TITLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.OntologyUtils;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.XMLUtils;

public class RiskFactorsToInstances {
	private static final String DEFAULT_DOCUMENT_SUFFIX = ".txt";
	private Map<String,IClass> schemaMap;
	private IOntology ontology;
	
	public static void main(String[] args) throws IOntologyException, IOException, TerminologyException {
		if(args.length > 2){
			String xmlDirectory = args[0];
			String ontology = args[1];
			String instances = args[2];
			
			RiskFactorsToInstances r2i = new RiskFactorsToInstances(ontology);
			r2i.convert(xmlDirectory,instances);
			
		}else{
			System.err.println("Usage: "+RiskFactorsToInstances.class.getSimpleName()+" <xml directory> <ontology> <instances>");
		}

	}
	
	public RiskFactorsToInstances(String ontologyLocation) throws IOntologyException{
		System.out.println("loading ontology .. "+ontologyLocation);
		File parentOntology = new File(ontologyLocation);
		ontology = OOntology.createOntology(createOntologyInstanceURI(parentOntology), parentOntology);
	}
	
	/**
	 * create ontology instance URI
	 * @param file
	 * @return
	 * @throws IOntologyException
	 */
	private URI createOntologyInstanceURI(File file) throws IOntologyException{
		String ontologyURI = null;
		try {
			ontologyURI = ""+OntologyUtils.getOntologyURI(file);
		} catch (IOException e) {
			throw new IOntologyException("Unable get parent ontology URL "+file);
		}
		if(ontologyURI.endsWith(".owl"))
			ontologyURI = ontologyURI.substring(0,ontologyURI.length()-4);
		ontologyURI += "Instances.owl";
		return URI.create(ontologyURI);
	}

	/**
	 * convert RiskFactors directory of XML gold annotations to instnaces in a target ontology
	 * @param xmlDirectory - location of annotated XML documents
	 * @param ontology - ontology to map to
	 * @param instances - output instnace ontology
	 * @throws IOntologyException  - exception that may be thrown
	 * @throws IOException 
	 * @throws TerminologyException 
	 */
	public void convert(String xmlDirectory, String outputFile) throws IOntologyException, IOException, TerminologyException {
	
		// go over anafora directory
		for(File xmlFile : new File(xmlDirectory).listFiles()){
			if(xmlFile.isFile() && xmlFile.getName().endsWith(".xml")){
				System.out.println("converting "+xmlFile.getName()+" ..");
				addDocumentInstance(xmlFile,ontology);
			}
		}
		// write ontology
		System.out.println("writing ontology .. "+outputFile);
		ontology.write(new FileOutputStream(outputFile),IOntology.OWL_FORMAT);
		System.out.println("ok");
		
	}
	
	
	/**
	 * parse Anafora annotations from XML
	 * @param dom
	 * @return
	 */
	private Map<String, Entity> parseAnnotations(Document dom,String documentName) {
		Map<String,Entity> map = new LinkedHashMap<String, Entity>();
		Element annotations = XMLUtils.getElementByTagName(dom.getDocumentElement(),"TAGS");
		for(Element el: XMLUtils.getChildElements(annotations)){
			// those are document level
			for(Element ml: XMLUtils.getChildElements(el)){
				Entity entity = Entity.load(ml);
				entity.put("document",documentName);
				map.put(entity.get("id"),entity);
			}	
		}
		return map;
	}
	/**
	 * define Anafora annotation entity
	 * @author tseytlin
	 */
	private static class Entity {
		public Map<String,String> properties = new LinkedHashMap<String,String>();
		
		/**
		 * load entity form XML element
		 * @param el - DOM element for Anafora entity
		 * @return entity element
		 */
		public static Entity load(Element el){
			Entity e = new Entity();
			e.properties.put("tag",el.getTagName());
			e.properties.putAll(XMLUtils.getAttributes(el));
			return e;
		}
		public String get(String name){
			return properties.get(name);
		}
		public void put(String key, String val){
			properties.put(key,val);
		}
		public String toString(){
			return properties.toString();
		}
		public String getTag(){
			return get("tag");
		}
		public String getID() {
			return get("document")+"_"+get("id");
		}
	}

	private void addDocumentInstance(File xmlFile, IOntology ontology) throws FileNotFoundException, IOException, TerminologyException, IOntologyException {
		String documentTitle = xmlFile.getName();
		if(documentTitle.endsWith(".xml"))
			documentTitle = documentTitle.substring(0,documentTitle.length()-4);
		
		String documentName=documentTitle;
		
		// add .txt suffix to align on name
		if(!documentTitle.endsWith(DEFAULT_DOCUMENT_SUFFIX))
			documentTitle = documentTitle+DEFAULT_DOCUMENT_SUFFIX;
		
		// get document text
		//String docText = FileTools.getText(new FileInputStream(docFile));
		Document dom = XMLUtils.parseXML(new FileInputStream(xmlFile));
		Map<String,Entity> annotations = parseAnnotations(dom,documentName);
		
		
		// create an instance
		IInstance composition = ontology.getClass(COMPOSITION).createInstance(OntologyUtils.toResourceName(documentTitle));
		composition.addPropertyValue(ontology.getProperty(HAS_TITLE),documentTitle);
		
		// process annotations
		for(String id: annotations.keySet()){
			Entity entity = annotations.get(id);
			IClass cls = getClass(entity);
			if(cls !=  null && cls.hasSuperClass(ontology.getClass(ANNOTATION))){
				IInstance mentionAnnotation = getInstance(cls,entity,annotations,ontology);
				// add annotations
				if(mentionAnnotation != null){
					composition.addPropertyValue(ontology.getProperty(HAS_MENTION_ANNOTATION),mentionAnnotation);
				}
			}else{
				System.out.println("WARNING: skipped "+entity);
			}
		}
		
	}

	private Map<String, IClass> getSchemaMap() {
		if(schemaMap == null){
			schemaMap = new HashMap<String, IClass>();
			/*schemaMap.put("MEDICATION",ontology.getClass("MedicationStatement"));
			schemaMap.put("SMOKER",ontology.getClass("smoker_mention"));
			schemaMap.put("HYPERTENSION",ontology.getClass("Hypertension_mention"));
			schemaMap.put("CAD",ontology.getClass("CAD_mention"));
			schemaMap.put("DIABETES",ontology.getClass("Diabetes_mention"));
			schemaMap.put("HYPERLIPIDEMIA",ontology.getClass("Hyperlipidemia_mention"));
			schemaMap.put("OBESE",ontology.getClass("Obesity_mention"));*/
			
			// hard-code some values
			schemaMap.put("after DCT",ontology.getClass("After_DocTimeRel"));
			schemaMap.put("before DCT",ontology.getClass("Before_DocTimeRel"));
			schemaMap.put("during DCT",ontology.getClass("Overlap_DocTimeRel"));
			
			// load up everything by label
			for(IClass cls: ontology.getClass("Annotation").getSubClasses()){
				schemaMap.put(cls.getLabel().toLowerCase(),cls);
			}
			
		}
		return schemaMap;
	}

	private IInstance getInstance(IClass cls,Entity entity, Map<String, Entity> annotations, IOntology ontology) throws TerminologyException, IOException, IOntologyException {
		IInstance inst = cls.createInstance(cls.getName()+"_"+entity.getID());
		IProperty hasSpan = ontology.getProperty("hasSpan");
		
		if(entity.get("start") != null && entity.get("end") != null)
			inst.addPropertyValue(hasSpan,entity.get("start")+":"+entity.get("end"));
		if(entity.get("time") != null){
			IClass time = getSchemaMap().get(entity.get("time"));
			if(time != null){
				inst.addPropertyValue(ontology.getProperty("hasTemporality"),getDefaultInstance(time));
			}
		}
		if(entity.get("text") != null){
			IProperty hasText = ontology.getProperty("hasAnnotationText");
			inst.addPropertyValue(hasText,entity.get("text"));
		}
		return inst;
	}

	/**
	 * get a class for a given entity
	 * @param entity
	 * @return
	 * @throws TerminologyException
	 * @throws IOException
	 * @throws IOntologyException
	 */
	private IClass getClass(Entity entity) throws TerminologyException, IOException, IOntologyException {
		String tag = entity.getTag();
		String indicator = entity.get("indicator");
		String type = entity.get("type1");
		String status = entity.get("status");
		String label = null;
		
		// now look at indicator
		if(indicator != null){
			/*
			</CAD>  </DIABETES> </HYPERLIPIDEMIA> </HYPERTENSION> </OBESE> 	
			/* indicator="A1C"  indicator="BMI" indicator="event" indicator="glucose" indicator="high bp"
			indicator="high chol." indicator="high LDL"  indicator="mention" indicator="not present"
			indicator="present" indicator="symptom" indicator="test"
			*/
			// if mention
			indicator = indicator.replaceAll("[^A-Za-z0-9 ]+","");
			tag= tag.replaceAll("_"," ");
			label = tag+("mention".equals(indicator)?"":" "+indicator)+" mention";
		}else if(status != null){
			/* SMOKER: 	status="current" status="ever" status="never" status="past"  status="unknown"  */
			label = tag+" "+status+" mention";
		}else if(type != null){
			/* MEDICATION: type1="ACE inhibitor" type1="ARB" type1="aspirin" type1="beta blocker" type1="calcium channel blocker"
				type1="diuretic" type1="DPP4 inhibitors" type1="ezetimibe" type1="fibrate" type1="insulin" type1="metformin"
				type1="niacin" type1="nitrate" type1="statin" type1="sulfonylureas" type1="thiazolidinedione" type1="thienopyridine"
			 */
			label = tag+" "+type+" mention";
		}
		
		// remember the class
		if(label != null){
			return getSchemaMap().get(label.toLowerCase());
		}
	
		return null;
	}

	private IInstance getDefaultInstance(IClass time) {
		String name = time.getName()+"_default";
		IInstance inst = time.getOntology().getInstance(name);
		if(inst == null){
			inst = time.createInstance(name);
		}
		return inst;
	}

}
