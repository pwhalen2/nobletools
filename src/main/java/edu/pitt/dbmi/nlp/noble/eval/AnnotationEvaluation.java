package edu.pitt.dbmi.nlp.noble.eval;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.coder.model.Spannable;
import edu.pitt.dbmi.nlp.noble.mentions.model.DomainOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class AnnotationEvaluation {
	public static final String DISJOINT_SPANS = "\\s+"; // span seperate
	public static final String SPAN_SEPERATOR = ":";    //within a span Ex: 12:45
	
	
	public static boolean STRICT_VALUE_CALCULATION = false;
	public static boolean PRINT_RECORD_LEVEL_STATS = false;
	private Map<String,Double> attributeWeights;
	private Analysis analysis;
	
	public static void main(String[] args) throws Exception {
		// compare two files
		if(args.length >= 2){
			File gold = null, candidate = null, weights = null;
			for(String s: args){
				if("-strict".equals(s)){
					STRICT_VALUE_CALCULATION = true;
				}else if("-print".equals(s)){
					PRINT_RECORD_LEVEL_STATS = true;
				}else if(gold  == null){
					gold = new File(s);
				}else if(candidate == null){
					candidate = new File(s);
				}else if(weights == null){
					weights = new File(s);
				}
			}
			
			AnnotationEvaluation pe = new AnnotationEvaluation();
			pe.loadWeights(weights);
			pe.evaluate(gold,candidate);
			pe.outputHTML(candidate.getParentFile());
		}else{
			System.err.println("Usage: java "+AnnotationEvaluation.class.getSimpleName()+" [-print|-strict] <gold instance owl file> <system instance owl file> [weights file]");
		}
	}

	/**
	 * output HTML files to a given parent directory
	 * @param parentFile
	 */
	public void outputHTML(File parentFile) throws IOException {
		HTMLExporter exporter = new HTMLExporter(parentFile);
		exporter.export(getAnalysis());
	}

	/**
	 * get analysis object for a given evaluation
	 * @return analysis object
	 */
	public Analysis getAnalysis(){
		if(analysis == null)
			analysis = new Analysis();
		return  analysis;
	}

	/**
	 * load attribute weights
	 * @param weights
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws NumberFormatException 
	 */
	public void loadWeights(File weights) throws NumberFormatException, FileNotFoundException, IOException {
		if(weights != null){
			for(String l: TextTools.getText(new FileInputStream(weights)).split("\n")){
				String [] p = l.split("\t");
				if(p.length == 2){
					getAttributeWeights().put(p[0].trim(),Double.parseDouble(p[1]));
				}
			}
		}
	}
	
	
	/**
	 * get attribute weights loaded from the corpus
	 * @return mapping of attribute name to its weight
	 */
	public Map<String, Double> getAttributeWeights() {
		if(attributeWeights == null)
			attributeWeights = new LinkedHashMap<String, Double>();
		return attributeWeights;
	}

	
	/**
	 * evaluate phenotype of two BSV files
	 * @param file1
	 * @param file2
	 * @throws IOException 
	 * @throws IOntologyException 
	 */
	
	private void evaluate(File file1, File file2) throws IOException, IOntologyException {
		IOntology goldInstances = OOntology.loadOntology(file1);
		IOntology systemInstances = OOntology.loadOntology(file2);
		
		// init confusionMatrix
		Analysis analysis = getAnalysis();
		analysis.setTitle("Results for "+file1.getName()+" on "+new Date());
		Analysis.ConfusionMatrix mentionConfusion = analysis.getConfusionMatrix("Mention");
		Analysis.ConfusionMatrix documentConfusion = analysis.getConfusionMatrix("Document");

		
		// get composition
		List<IInstance> goldCompositions = getCompositions(goldInstances);
		List<IInstance> candidateCompositions = getCompositions(systemInstances);
		

		for(IInstance gold: goldCompositions){
			IInstance cand = getMatchingComposition(candidateCompositions,gold);
			if(cand != null){
				calculateDocumentConfusion(gold, cand,DomainOntology.HAS_MENTION_ANNOTATION,mentionConfusion);
				calculateDocumentConfusion(gold, cand,DomainOntology.HAS_DOCUMENT_ANNOTATION,documentConfusion);
			}
		}
		
		// print results
		analysis.printResultTable(System.out);
	}

	public static boolean isPrintErrors(){
		return PRINT_RECORD_LEVEL_STATS;
	}
	public static boolean isStrict(){
		return STRICT_VALUE_CALCULATION;
	}
	
	/**
	 * calculate confusion for two composition on a given annotation type
	 * @param gold - gold document instance
	 * @param system - system document instance
	 * @param prop - property for a type of annotations to fetch
	 * @param confusion - total confusion matrix
	 */
	private void calculateDocumentConfusion(IInstance gold, IInstance system, String prop, Analysis.ConfusionMatrix confusion){
		// get a list of gold variables and system vars for each document
		String docTitle = getDocumentTitle(gold);
		List<IInstance> goldVariables = getAnnotationVariables(gold,gold.getOntology().getProperty(prop));
		List<IInstance> systemVariables = getAnnotationVariables(system,system.getOntology().getProperty(prop));
		
		Set<IInstance> usedSystemCandidates = new HashSet<IInstance>();
		
		for(IInstance goldInst: goldVariables){
			Analysis.ConfusionMatrix varConfusion = getConfusionMatrix(goldInst);
			List<IInstance> sysInstances = getMatchingAnnotationVaiables(systemVariables,goldInst);
			if(sysInstances.isEmpty()){
				confusion.FN ++;
				varConfusion.FN++;
				getAnalysis().addError(confusion.getLabelFN(),docTitle,goldInst);
				getAnalysis().addError(varConfusion.getLabelFN(),docTitle,goldInst);
			}else{
				for(IInstance sysInst : sysInstances ){
					usedSystemCandidates.add(sysInst);
					double score = getWeightedScore(goldInst,sysInst);
					confusion.TPP ++;
					confusion.TP += score;

					varConfusion.TPP ++;
					varConfusion.TP += score;

				}
			}
		}
		for(IInstance inst: systemVariables){
			if(!usedSystemCandidates.contains(inst)){
				confusion.FP ++;
				getConfusionMatrix(inst).FP++;
				getAnalysis().addError(confusion.getLabelFP(),docTitle,inst);
				getAnalysis().addError(getConfusionMatrix(inst).getLabelFP(),docTitle,inst);
			}
		}
	}

	/**
	 * get document title
	 * @param doc
	 * @return
	 */
	private String getDocumentTitle(IInstance doc) {
		IProperty title = doc.getOntology().getProperty(DomainOntology.HAS_TITLE);
		return (String) doc.getPropertyValue(title);
	}

	/**
	 * get confusion matrix for a given type of instance
	 * @param goldInst
	 * @return
	 */
	private Analysis.ConfusionMatrix getConfusionMatrix(IInstance goldInst) {
		String name = goldInst.getDirectTypes()[0].getName();
		return getAnalysis().getConfusionMatrix(name);
	}


	private List<IInstance> getMatchingAnnotationVaiables(List<IInstance> candidateVariables, IInstance goldInst) {
		List<IInstance> matchedInstances = new ArrayList<IInstance>();
		IProperty prop = null; 
		String goldSpan = ""+goldInst.getPropertyValue(goldInst.getOntology().getProperty(DomainOntology.HAS_SPAN));
		IClass goldType = goldInst.getDirectTypes()[0];
		
		// set to the percent of overlap of gold 
		double overlapThreshold = 0;
		
		// go through all possible candidate variables and select the ones that are the same type (or more specific)
		// and have a span overlap above threshold
		for(IInstance inst: candidateVariables){
			if(prop == null)
				prop = inst.getOntology().getProperty(DomainOntology.HAS_SPAN);
			String span = ""+inst.getPropertyValue(prop);
			IClass type = inst.getDirectTypes()[0];
			// if candidate type is identical to gold or more specific
			if(type.equals(goldType) || type.hasSuperClass(goldType)){
				int overlap = spanOverlap(goldSpan,span);
				if(overlap > overlapThreshold){
					matchedInstances.add(inst);
				}
			}
			
		}
		return matchedInstances;
	}

	/**
	 * do spans overlap on anchor
	 * @param goldSpan
	 * @param candidateSpan
	 * @return
	 */
	private int spanOverlap(String goldSpan, String candidateSpan) {
		int overlap = 0;
		List<Span> goldSpans = parseSpans(goldSpan);
		List<Span> candidateSpans = parseSpans(candidateSpan);
		for(Span sp: goldSpans){
			for(Span csp: candidateSpans){
				// if GOLD overlaps any candidate span, BINGO, we got it for now ..
				overlap += sp.overlapLength(csp);		
			}
		}
		return overlap;
	}

	public static class Span {
		public int start,end;
		public static Span getSpan(String st, String en){
			Span sp = new Span();
			sp.start = Integer.parseInt(st);
			sp.end = Integer.parseInt(en);
			return sp;
		}
		public static Span getSpan(String span){
			String [] p = span.split(SPAN_SEPERATOR);
			if(p.length == 2){
				return getSpan(p[0],p[1]);
			}
			return null;
		}
		
		public boolean overlaps(Span s){
			if(s == null)
				return false;
			//NOT this region ends before this starts or other region ends before this one starts
			return !(end < s.start || s.end < start);
		}
		public int overlapLength(Span s){
			if(overlaps(s)){
				return Math.min(end,s.end) - Math.max(start,s.start);
			}
			return 0;
		}
	}
	
	
	/**
	 * pars spans from string
	 * @param text
	 * @return
	 */
	private List<Span> parseSpans(String text) {
		List<Span> list = new ArrayList<Span>();
		for(String span: text.split(DISJOINT_SPANS)){
			Span sp = Span.getSpan(span);
			if(sp != null){
				list.add(sp);
			}
		}
		return list;
	}

	private double getWeightedScore(IInstance goldInst, IInstance systemInst) {
		// we start with score of 1.0 cause we did match up the (anchor aprory)
		double defaultWeight = getDefaultWeight(goldInst);
		double numerator   = 1.0;  // initial weight of an anchor
		double denominator = 1.0;  // initial total score 
		for(IProperty goldProp: getProperties(goldInst)){
			for(IInstance gVal: getInstanceValues(goldInst.getPropertyValues(goldProp))){
				double weight = getWeight(gVal);
				if(weight == 0)
					weight = defaultWeight;
				denominator += weight;
				numerator += weight * hasAttributeValue(systemInst,goldProp,gVal);
			}
		}
		return numerator / denominator;
	}

	private double getDefaultWeight(IInstance inst){
		double count = 0;
		for(IProperty goldProp: getProperties(inst)) {
			for (IInstance gVal : getInstanceValues(inst.getPropertyValues(goldProp))) {
				count++;
			}
		}
		return count > 0?1.0 / count:0;
	}

	/**
	 * does a system instance have a given value
	 * @param systemInst - system instnce
	 * @param prop - property
	 * @param goldValue - gold value
	 * @return 1 or 0
	 */
	private int hasAttributeValue(IInstance systemInst, IProperty prop, IInstance goldValue){
		IClass goldValueClass = goldValue.getDirectTypes()[0];
		prop = systemInst.getOntology().getProperty(prop.getName());
		for(IInstance val: getInstanceValues(systemInst.getPropertyValues(prop))){
			if(goldValueClass.equals(val.getDirectTypes()[0])){
				return 1;
			}
		}
		return 0;
	}

	private double getWeight(IInstance inst){
		String name = inst.getDirectTypes()[0].getName();
		if(getAttributeWeights().containsKey(name)){
			return getAttributeWeights().get(name);
		}
		// default weight in case we don't have a good one
		System.err.println("no weight for: "+name);
		return 0;
	}

	private List<IProperty> getProperties(IInstance inst){
		List<IProperty> props = new ArrayList<IProperty>();
		for(IProperty p: inst.getProperties()){
			if(p.isObjectProperty()){
				props.add(p);
			}
		}
		return props;
	}
	
	private List<IInstance> getInstanceValues(Object [] objects ){
		List<IInstance> list = new ArrayList<IInstance>();
		for(Object o: objects){
			if(o instanceof IInstance){
				list.add((IInstance)o);
			}
		}
		return list;
	}
	

	/**
	 * select matching composition
	 * @param candidateCompositions
	 * @param gold
	 * @return
	 */
	private IInstance getMatchingComposition(List<IInstance> candidateCompositions, IInstance gold) {
		IProperty prop = null; 
		String goldTitle = ""+gold.getPropertyValue(gold.getOntology().getProperty(DomainOntology.HAS_TITLE));
		for(IInstance inst: candidateCompositions){
			if(prop == null)
				prop = inst.getOntology().getProperty(DomainOntology.HAS_TITLE);
			String title = ""+inst.getPropertyValue(prop);
			if(goldTitle.equals(title))
				return inst;
		}
		return null;
	}

	/**
	 * get annotation variables of a composition instance
	 * @param composition
	 * @param prop - property
	 * @return
	 */
	private List<IInstance> getAnnotationVariables(IInstance composition, IProperty prop) {
		List<IInstance> list = new ArrayList<IInstance>();
		for(Object o: composition.getPropertyValues(prop)){
			if(o instanceof IInstance)
				list.add((IInstance)o);
		}
		return list;
	}

	
	/**
	 * get composition instances
	 * @param ont - ontology
	 * @return list of composition instances
	 */
	private List<IInstance> getCompositions(IOntology ont) {
		List<IInstance> list = new ArrayList<IInstance>();
		for(IInstance inst: ont.getClass(DomainOntology.COMPOSITION).getInstances()){
			list.add(inst);
		}
		return list;
	}
	

}
