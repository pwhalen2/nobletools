package edu.pitt.dbmi.nlp.noble.mentions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import edu.pitt.dbmi.nlp.noble.coder.NobleCoder;
import edu.pitt.dbmi.nlp.noble.coder.model.*;
import edu.pitt.dbmi.nlp.noble.coder.processor.ReportProcessor;
import edu.pitt.dbmi.nlp.noble.mentions.model.Instance;
import edu.pitt.dbmi.nlp.noble.mentions.model.AnnotationVariable;
import edu.pitt.dbmi.nlp.noble.mentions.model.Composition;
import edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.tools.ConText;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.ui.TerminologyBrowser;

public class NobleMentions implements Processor<Composition>{
	private long time;
	private DomainOntology domainOntology;
	private NobleCoder coder;
	
	
	/**
	 * 
	 * @param args
	 * @throws TerminologyException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws IOntologyException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, TerminologyException, IOntologyException {
		File file = new File("/home/tseytlin/Data/BiRADS/pathology/reports/realDCIS.txt");
		DomainOntology domainOntology = new DomainOntology("/home/tseytlin/Data/BiRADS/ontology/pathologicDx.owl");
		NobleMentions noble = new NobleMentions(domainOntology);
		
		if(args.length == 0){
			Composition doc = noble.process(file);
			for(AnnotationVariable var: doc.getAnnotationVariables()){
				System.out.println(var);
			}
			System.out.println(doc.getProcessTime());
			
			// visualize terminologies
			TerminologyBrowser browser = new TerminologyBrowser();
			browser.setTerminologies(domainOntology.getTerminologies());
			browser.showDialog(null,"NobleMentions");
		}
		
		if(args.length > 0){
			final String I = "|";
			File [] files = new File(args[0]).listFiles();
			Arrays.sort(files);
			for(File f: files){
				if(f.getName().endsWith(".txt")){
					Composition doc = noble.process(f);
					for(AnnotationVariable var: doc.getAnnotationVariables()){
						for(Instance body: var.getModifierInstances("hasBodySite")){
							System.out.println(f.getName()+I+var.getName()+I+body.getName()+I+toString(body.getModifierInstances("hasBodySide"))+I+toString(body.getModifierInstances("hasClockfacePosition"))+I+var.getAnnotations());
						}
					}
				}
			}
		}
	}

	private static String toString(Collection obj){
		if(obj == null)
			return "";
		String s = obj.toString();
		return s.substring(1, s.length()-1);
	}
	
	
	/**
	 * initialize noble mentions with initialized domain ontology
	 * @param ontoloy
	 */
	public NobleMentions(DomainOntology ontoloy){
		setDomainOntology(ontoloy);
	}
	
	/**
	 * get domain ontology
	 * @return
	 */
	public DomainOntology getDomainOntology() {
		return domainOntology;
	}

	/**
	 * set domain ontology
	 * @param domainOntology
	 */
	public void setDomainOntology(DomainOntology domainOntology) {
		this.domainOntology = domainOntology;
		
		// initialize noble coder
		coder = new NobleCoder(domainOntology.getAnchorTerminology());
		coder.setAcronymExpansion(false);
		coder.setContextDetection(true);
		coder.setDocumentProcessor(new ReportProcessor());
		
		// initialize context
		ConText conText = new ConText(domainOntology.getModifierTerminology());
		conText.setModifierValidator(domainOntology.getModifierValidator());
		coder.setConText(conText);
		//coder.setDocumentProcessor(documentProcessor);
	}

	/**
	 * process document represented as a string.
	 *
	 * @param document the document
	 * @return the document
	 * @throws FileNotFoundException the file not found exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws TerminologyException the terminology exception
	 */
	public Composition process(File document) throws FileNotFoundException, IOException, TerminologyException {
		Composition doc = new Composition(TextTools.getText(new FileInputStream(document)));
		doc.setDomainOntology(domainOntology);
		doc.setLocation(document.getAbsolutePath());
		doc.setTitle(document.getName());
		return process(doc);
	}
	
	
	public Composition process(Composition doc) throws TerminologyException {
		time = System.currentTimeMillis();
		
		// run coder: sentence parser + dictionary lookup + ConText on the document
		coder.process(doc);
		
		// gether all global modifiers that need to be resolved beyound sentence boundaries
		List<Mention> globalModifiers = getGlobalModifiers(doc);
		
		// now lets construct annotation variables from anchor mentions
		for(Sentence sentence: doc.getSentences()){
			for(Instance anchor: domainOntology.getAnchors(sentence.getMentions())){
				for(AnnotationVariable var : domainOntology.getAnnotationVariables(anchor)){
					
					// associate with global modifiers
					for(Modifier modifier: coder.getConText().getMatchingModifiers(globalModifiers,var.getAnchor().getMention())){
						var.addModifier(modifier);
					}
					
					//check if property is fully satisfied
					if(var.isSatisfied()){
						doc.addAnnotationVariable(var);
					}
				}
			}
		}
		
		
		time = System.currentTimeMillis() - time;
		doc.getProcessTime().put(getClass().getSimpleName(),time);
		return doc;
	}

	
	/**
	 * get all global modifiers that can be associated outside sentence boundaries
	 * @param doc
	 * @return
	 */
	private List<Mention> getGlobalModifiers(Document doc){
		List<Mention> globalModifiers = new ArrayList<Mention>();
		for(Mention m: doc.getMentions()){
			if(domainOntology.isTypeOf(m,DomainOntology.MODIFIER)){
				globalModifiers.add(m);
			}
		}
		return globalModifiers;
	}
	
	public long getProcessTime() {
		return time;
	}

}
