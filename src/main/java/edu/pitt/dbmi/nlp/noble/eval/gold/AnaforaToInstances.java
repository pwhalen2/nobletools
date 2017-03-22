package edu.pitt.dbmi.nlp.noble.eval.gold;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology.*;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.OntologyUtils;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.FileTools;
import edu.pitt.dbmi.nlp.noble.util.XMLUtils;


/**
 * convert Anafora XML to OWL instances of a given ontology
 * @author tseytlin
 *
 */
public class AnaforaToInstances {
	private final String DEFAULT_DOCUMENT_SUFFIX = ".txt"; 
	
	public static void main(String [] args) throws Exception{
		if(args.length > 3){
			String anaforaDirectory = args[0];
			String anaforaSuffix = args[1];
			String ontology = args[2];
			String instances = args[3];
			
			AnaforaToInstances a2i = new AnaforaToInstances();
			a2i.convert(anaforaDirectory,anaforaSuffix,ontology,instances);
			
		}else{
			System.err.println("Usage: "+AnaforaToInstances.class.getSimpleName()+" <anafora directory> <annotation suffix> <ontology> <instances>");
		}
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
	 * convert anafora directory into instantiated ontology
	 * @param anaforaDirectory - anafora directory where the annotated files are
	 * @param anaforaSuffix - anafora suffix for annotated files
	 * @param ontology - ontology to map to
	 * @param instances - instantiated ontology where we'll write the output
	 * @throws IOntologyException 
	 * @throws IOException 
	 */
	public void convert(String anaforaDirectory, String anaforaSuffix, String ontolgyLocation, String outputFile) throws IOntologyException, IOException {
		System.out.println("loading ontology .. "+ontolgyLocation);
		File parentOntology = new File(ontolgyLocation);
		IOntology ontology = OOntology.createOntology(createOntologyInstanceURI(parentOntology), parentOntology);
		// go over anafora directory
		for(File docDir : new File(anaforaDirectory).listFiles()){
			if(docDir.isDirectory()){
				String docName = docDir.getName();
				File docFile = new File(docDir,docName);
				File xmlFile = new File(docDir,docName+anaforaSuffix);
				if(docFile.exists() && xmlFile.exists()){
					System.out.println("converting "+docName+" ..");
					addDocumentInstance(docFile,xmlFile,ontology);
				}
			}
		}
		
		// write ontology
		System.out.println("writing ontology .. "+outputFile);
		ontology.write(new FileOutputStream(outputFile),IOntology.OWL_FORMAT);
		System.out.println("ok");
	}

	/**
	 * define Anafora annotation entity
	 * @author tseytlin
	 */
	private static class Entity {
		public String id,span,type,parentsType;
		public Map<String,Set<String>> properties;
		/**
		 * load entity form XML element
		 * @param el - DOM element for Anafora entity
		 * @return entity element
		 */
		public static Entity load(Element el){
			Entity e = new Entity();
			for(Element i: XMLUtils.getChildElements(el,"id")){
				e.id = i.getTextContent().trim(); break;
			}
			for(Element i: XMLUtils.getChildElements(el,"span")){
				e.span = i.getTextContent().trim(); break;
			}
			for(Element i: XMLUtils.getChildElements(el,"type")){
				e.type = i.getTextContent().trim(); break;
			}
			for(Element i: XMLUtils.getChildElements(el,"parentsType")){
				e.parentsType = i.getTextContent().trim(); break;
			}
			for(Element i: XMLUtils.getChildElements(el,"properties")){
				for(Element j: XMLUtils.getChildElements(i)){
					e.addProperty(j.getTagName(),j.getTextContent().trim());
				}
			}
			return e;
		}
		/**
		 * add property
		 * @param tagName
		 * @param trim
		 */
		private void addProperty(String name, String value) {
			if(properties == null)
				 properties = new LinkedHashMap<String,Set<String>>();
			Set<String> set = properties.get(name);
			if(set == null){
				set = new LinkedHashSet<String>();
				properties.put(name,set);
			}
			set.add(value);
		}
		public String getProperty(String name) {
			Set<String> vals = properties.get(name);
			if(vals != null)
				vals.iterator().next();
			return null;
		}
		public Set<String> getProperties(String name) {
			Set<String> vals = properties.get(name);
			if(vals != null)
				return vals;
			return Collections.EMPTY_SET;
		}
	}
	
	/**
	 * parse Anafora annotations from XML
	 * @param dom
	 * @return
	 */
	private Map<String, Entity> parseAnnotations(Document dom) {
		Map<String,Entity> map = new LinkedHashMap<String, AnaforaToInstances.Entity>();
		Element annotations = XMLUtils.getElementByTagName(dom.getDocumentElement(),"annotations");
		for(Element el: XMLUtils.getChildElements(annotations,"entity")){
			Entity entity = Entity.load(el);
			map.put(entity.id,entity);
		}
		return map;
	}
	
	
	/**
	 * add document instance from annotated xmlFile to ontology
	 * @param docFile - a document containing report text (just in case)
	 * @param xmlFile - an xml document containing annotations
	 * @param ontology - ontology that we are mapping things to
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	private void addDocumentInstance(File docFile, File xmlFile, IOntology ontology) throws FileNotFoundException, IOException {
		String documentTitle = docFile.getName();
		// add .txt suffix to align on name
		if(!documentTitle.endsWith(DEFAULT_DOCUMENT_SUFFIX))
			documentTitle = documentTitle+DEFAULT_DOCUMENT_SUFFIX;
		
		// get document text
		//String docText = FileTools.getText(new FileInputStream(docFile));
		Document dom = XMLUtils.parseXML(new FileInputStream(xmlFile));
		Map<String,Entity> annotations = parseAnnotations(dom);
		Map<String,IClass> schemaMap = getSchemaMap(ontology);
		
		// create an instance
		IInstance composition = ontology.getClass(COMPOSITION).createInstance(OntologyUtils.toResourceName(documentTitle));
		composition.addPropertyValue(ontology.getProperty(HAS_TITLE),documentTitle);
		
		// process annotations
		for(String id: annotations.keySet()){
			Entity entity = annotations.get(id);
			if(schemaMap.containsKey(entity.type)){
				IInstance mentionAnnotation = getInstance(entity,annotations,ontology);
				// add annotations
				if(mentionAnnotation != null && ontology.getClass(ANNOTATION).hasSubClass(mentionAnnotation.getDirectTypes()[0])){
					composition.addPropertyValue(ontology.getProperty(HAS_MENTION_ANNOTATION),mentionAnnotation);
				}
			}
		}
		
	}

	/**
	 * create annotation instance variable if possible
	 * @param entity - Anafaro entity
	 * @param annotations - list of anafora entities
	 * @param ontology - ontology
	 * @return instance
	 */
	private IInstance getInstance(Entity entity, Map<String, Entity> annotations, IOntology ontology) {
		// get instances if already defined
		String name = OntologyUtils.toResourceName(entity.id);
		if(ontology.hasResource(name))
			return ontology.getInstance(name);
		
		// need to find annotation class first
		IClass typeClass = getSchemaMap(ontology).get(entity.type);
		String code = entity.getProperty("associatedCode");
		if(code != null && typeClass != null){
			IClass cls = findMatchingClass(typeClass,code);
			if(cls != null){
				IInstance inst = cls.createInstance(name);
				
				
				return inst;
			}
		}
		return null;
	}

	private IClass findMatchingClass(IClass typeClass, String code) {
		// TODO Auto-generated method stub
		return null;
	}

	private Map<String, IClass> getSchemaMap(IOntology ontology) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
}
