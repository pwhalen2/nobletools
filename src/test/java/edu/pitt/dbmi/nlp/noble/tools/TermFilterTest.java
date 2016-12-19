package edu.pitt.dbmi.nlp.noble.tools;

import java.io.IOException;
import java.util.*;

import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;

public class TermFilterTest {

	/**
	 * Test term filter
	 */
	public void testTermFilter(){
		
		List<String> terms = Arrays.asList(
				"Alzheimer’s disease”","Failure, Renal","Selective Serotonin Reuptake Inhibitors (SSRIs)","Chondria <beetle>","Surgical intervention (finding)",
				"[V] Alcohol use","10*9/L","ADHESIVE @@ BANDAGE","EC 2.7.1.112","Unclassified sequences","Melanoma,NOS","Other findings","Head and Neck Squamous Cell Carcinoma",
				"structure of breast","entire breast","left breast");
		System.out.println("before:\t"+terms);
		System.out.println("after:\t"+TermFilter.filter(terms));
	}
	
	public void testWithTerminology() throws IOException, TerminologyException{
		NobleCoderTerminology term = new NobleCoderTerminology("NCI_Metathesaurus");
		for(String code: Arrays.asList("C0025202","C0005823")){
			Concept c = term.lookupConcept(code);
			if(c != null){
				System.out.println(c.getName()+" ("+code+")");
				System.out.println("synonyms before:\t"+Arrays.asList(c.getSynonyms()));
				System.out.println("synonyms after:\t"+TermFilter.filter(c.getSynonyms()));
			}
		}
	}
	
	public static void main(String[] args) throws IOException, TerminologyException {
		TermFilterTest test = new TermFilterTest();
		test.testTermFilter();
		test.testWithTerminology();

	}

}
