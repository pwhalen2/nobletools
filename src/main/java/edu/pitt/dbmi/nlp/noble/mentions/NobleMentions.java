package edu.pitt.dbmi.nlp.noble.mentions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import edu.pitt.dbmi.nlp.noble.coder.NobleCoder;
import edu.pitt.dbmi.nlp.noble.coder.model.*;
import edu.pitt.dbmi.nlp.noble.mentions.model.Anchor;
import edu.pitt.dbmi.nlp.noble.mentions.model.AnnotationVariable;
import edu.pitt.dbmi.nlp.noble.mentions.model.Composition;
import edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.tools.ConText;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

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
		File file = new File("/home/tseytlin/Data/BiRADS/pathology/reports/patientX_doc2_SP.txt");
		DomainOntology domainOntology = new DomainOntology("/home/tseytlin/Data/BiRADS/ontology/pathologicDx.owl");
		NobleMentions noble = new NobleMentions(domainOntology);
		Composition doc = noble.process(file);
		for(AnnotationVariable var: doc.getAnnotationVariables()){
			System.out.println(var);
		}
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
		
		ConText conText = new ConText(domainOntology.getModifierTerminology());
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
		doc.setLocation(document.getAbsolutePath());
		doc.setTitle(document.getName());
		return process(doc);
	}
	
	
	public Composition process(Composition doc) throws TerminologyException {
		time = System.currentTimeMillis();
		
		// run coder: sentence parser + dictionary lookup + ConText on the document
		coder.process(doc);
		
		// now lets construct annotation variables from anchor mentions
		for(Sentence sentence: doc.getSentences()){
			for(Anchor anchor: domainOntology.getAnchors(sentence.getMentions())){
				for(AnnotationVariable var : domainOntology.getAnnotationVariables(anchor)){
					// perhaps associate other variables here
					//TODO: process stuff in paragraphs and such
					doc.addAnnotationVariable(var);
				}
			}
		}
		
		
		time = System.currentTimeMillis() - time;
		doc.getProcessTime().put(getClass().getSimpleName(),time);
		return doc;
	}

	public long getProcessTime() {
		return time;
	}

}
