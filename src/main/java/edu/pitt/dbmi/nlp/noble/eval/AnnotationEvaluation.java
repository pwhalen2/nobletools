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
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

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
	public static boolean LIST_RECORD_LEVEL_STATS = false;
	public static int MAX_ATTRIBUTE_SIZE = 10;
	private Map<String,Double> attributeWeights;
	private Map<String,ConfusionMatrix> confusions;
	
	
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
		}else{
			System.err.println("Usage: java "+AnnotationEvaluation.class.getSimpleName()+" [-print|-strict] <gold instance owl file> <system instance owl file> [weights file]");
		}
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



	public static enum ConfusionLabel {
		TP,FP,FN,TN
	}
	public static class ConfusionMatrix {
		public double TPP,TP,FP,FN,TN;
		public void append(ConfusionMatrix c){
			TPP += c.TPP;
			TP += c.TP;
			FP += c.FP;
			FN += c.FN;
			TN += c.TN;
		}
		
		public double getPrecision(){
			return TP / (TP+ FP);
		}
		public double getRecall(){
			return  TP / (TP+ FN);
		}
		public double getFscore(){
			double precision = getPrecision();
			double recall = getRecall();
			return (2*precision*recall)/(precision + recall);
		}
		public double getAccuracy(){
			return (TP+TN) / (TP+TN+FP+FN);
		}
		
		public static void printHeader(PrintStream out){
			out.println(String.format("%1$-"+MAX_ATTRIBUTE_SIZE+"s","Label")+"\tTP\tTP'\tFP\tFN\tTN\tPrecis\tRecall\tAccur\tF1-Score");
		}
		public void print(PrintStream out,String label){
			out.println(String.format("%1$-"+MAX_ATTRIBUTE_SIZE+"s",label)+"\t"+
					TextTools.toString(TP)+"\t"+TextTools.toString(TPP)+"\t"+TextTools.toString(FP)+"\t"+
					TextTools.toString(FN)+"\t"+TextTools.toString(TN)+"\t"+
					TextTools.toString(getPrecision())+"\t"+
					TextTools.toString(getRecall())+"\t"+
					TextTools.toString(getAccuracy())+"\t"+
					TextTools.toString(getFscore()));
		}
		public String toString(){
			return "TP: "+TP+" ,FP: "+FP+", FN: "+FN;
		}
	}
	
	
	
	private void append(Map<String,ConfusionMatrix> first, Map<String,ConfusionMatrix> second){
		for(String hd: second.keySet()){
			ConfusionMatrix c = first.get(hd);
			if(c == null){
				c = new ConfusionMatrix();
				first.put(hd,c);
			}
			c.append(second.get(hd));
		}
		
	}
	
	private static String toString(Collection c){
		if(c == null)
			return "";
		String s = c.toString();
		if(s.startsWith("[") && s.endsWith("]"))
			return s.substring(1,s.length()-1);
		return s;
	}
	
	
	/**
	 * get generated confusion matricies
	 * @return map of confusion matricies
	 */
	public Map<String,ConfusionMatrix> getConfusionMatricies(){
		if(confusions == null)
			confusions = new LinkedHashMap<String, AnnotationEvaluation.ConfusionMatrix>();
		return confusions;
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
		ConfusionMatrix mentionConfusion = new ConfusionMatrix();
		ConfusionMatrix documentConfusion = new ConfusionMatrix();
		getConfusionMatricies().put("Mention",mentionConfusion);
		getConfusionMatricies().put("Document",documentConfusion);
		
		
		// get composition
		List<IInstance> goldCompositions = getCompositions(goldInstances);
		List<IInstance> candidateCompositions = getCompositions(systemInstances);
		
		// init error storage
		Map<ConfusionLabel,List<IInstance>> errors = new LinkedHashMap<AnnotationEvaluation.ConfusionLabel, List<IInstance>>();
				
		for(IInstance gold: goldCompositions){
			IInstance cand = getMatchingComposition(candidateCompositions,gold);
			if(cand != null){
				calculateDocumentConfusion(gold, cand,DomainOntology.HAS_MENTION_ANNOTATION,mentionConfusion,errors);
				calculateDocumentConfusion(gold, cand,DomainOntology.HAS_DOCUMENT_ANNOTATION,documentConfusion,errors);
			}
		}
		
		// print results
		ConfusionMatrix.printHeader(System.out);
		for(String label: getConfusionMatricies().keySet()){
			getConfusionMatricies().get(label).print(System.out,label);
		}
	}

	public static boolean isPrintErrors(){
		return PRINT_RECORD_LEVEL_STATS;
	}
	public static boolean isStrict(){
		return STRICT_VALUE_CALCULATION;
	}
	
	/**
	 * calculate confusion for two composition on a given annotation type
	 * @param gold
	 * @param cand
	 * @param prop
	 * @param confusion
	 * @param errors
	 */
	private void calculateDocumentConfusion(IInstance gold, IInstance cand, String prop, ConfusionMatrix confusion, Map<ConfusionLabel,List<IInstance>> errors){
		if(!errors.containsKey(ConfusionLabel.FN))
			errors.put(ConfusionLabel.FN,new ArrayList<IInstance>());
		if(!errors.containsKey(ConfusionLabel.FP))
			errors.put(ConfusionLabel.FP,new ArrayList<IInstance>());
		
		// get a list of gold variables and system vars for each document
		List<IInstance> goldVariables = getAnnotationVariables(gold,gold.getOntology().getProperty(prop));
		List<IInstance> systemVariables = getAnnotationVariables(cand,cand.getOntology().getProperty(prop));
		
		Set<IInstance> usedSystemCandidates = new HashSet<IInstance>();
		
		for(IInstance goldInst: goldVariables){
			ConfusionMatrix varConfusion = getConfusionMatrix(goldInst);
			List<IInstance> sysInstances = getMatchingAnnotationVaiables(systemVariables,goldInst);
			if(sysInstances.isEmpty()){
				confusion.FN ++;
				varConfusion.FN++;
				errors.get(ConfusionLabel.FN).add(goldInst);
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
				errors.get(ConfusionLabel.FP).add(inst);
			}
		}
	}

	private ConfusionMatrix getConfusionMatrix(IInstance goldInst) {
		String name = goldInst.getDirectTypes()[0].getName();
		return getConfusionMatrix(name);
	}

	private ConfusionMatrix getConfusionMatrix(String name) {
		ConfusionMatrix confusion = getConfusionMatricies().get(name);
		if(confusion == null){
			confusion = new ConfusionMatrix();
			getConfusionMatricies().put(name,confusion);
		}
		return confusion;
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
