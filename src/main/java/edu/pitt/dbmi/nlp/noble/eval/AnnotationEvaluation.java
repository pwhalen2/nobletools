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
	
	public static void main(String[] args) throws Exception {
		// compare two files
		if(args.length >= 2){
			File gold = null, candidate = null;
			for(String s: args){
				if("-strict".equals(s)){
					STRICT_VALUE_CALCULATION = true;
				}else if("-print".equals(s)){
					PRINT_RECORD_LEVEL_STATS = true;
				}else if("-list".equals(s)){
					LIST_RECORD_LEVEL_STATS = true;
				}else if(gold  == null){
					gold = new File(s);
				}else if(candidate == null){
					candidate = new File(s);
				}
			}
			
			AnnotationEvaluation pe = new AnnotationEvaluation();
			pe.evaluate(gold,candidate);	
		}else{
			System.err.println("Usage: java "+AnnotationEvaluation.class.getSimpleName()+" [-print|-strict|-list] <gold .owl file> <candidate .owl file>");
		}
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
	 * evaluate phenotype of two BSV files
	 * @param file1
	 * @param file2
	 * @throws IOException 
	 * @throws IOntologyException 
	 */
	
	private void evaluate(File file1, File file2) throws IOException, IOntologyException {
		IOntology goldOntology = OOntology.loadOntology(file1);
		IOntology candidateOntology = OOntology.loadOntology(file2);
		
		// init confusionMatrix
		ConfusionMatrix mentionConfusion = new ConfusionMatrix();
		ConfusionMatrix documentConfusion = new ConfusionMatrix();
		
		// get composition
		List<IInstance> goldCompositions = getCompositions(goldOntology);
		List<IInstance> candidateCompositions = getCompositions(candidateOntology);
		
		// init error storage
		Map<ConfusionLabel,List<IInstance>> errors = new LinkedHashMap<AnnotationEvaluation.ConfusionLabel, List<IInstance>>();
				
		for(IInstance gold: goldCompositions){
			IInstance cand = getMatchingComposition(candidateCompositions,gold);
			if(cand != null){
				calculateConfusion(gold, cand,DomainOntology.HAS_MENTION_ANNOTATION,mentionConfusion,errors);
				calculateConfusion(gold, cand,DomainOntology.HAS_DOCUMENT_ANNOTATION,documentConfusion,errors);
			}
		}
		
		// print results
		ConfusionMatrix.printHeader(System.out);
		mentionConfusion.print(System.out,"Mention");
		documentConfusion.print(System.out,"Document");
	}

	
	/**
	 * calculate confusion for two composition on a given annotation type
	 * @param gold
	 * @param cand
	 * @param prop
	 * @param confusion
	 * @param errors
	 */
	private void calculateConfusion(IInstance gold, IInstance cand, String prop, ConfusionMatrix confusion, Map<ConfusionLabel,List<IInstance>> errors){
		if(!errors.containsKey(ConfusionLabel.FN))
			errors.put(ConfusionLabel.FN,new ArrayList<IInstance>());
		if(!errors.containsKey(ConfusionLabel.FP))
			errors.put(ConfusionLabel.FP,new ArrayList<IInstance>());
		
		List<IInstance> goldVariables = getAnnotationVariables(gold,gold.getOntology().getProperty(prop));
		List<IInstance> candidateVariables = getAnnotationVariables(cand,cand.getOntology().getProperty(prop));
		Set<IInstance> usedCandidates = new HashSet<IInstance>();
		for(IInstance goldInst: goldVariables){
			IInstance candInst = getMatchingAnnotationVaiable(candidateVariables,goldInst);
			if(candInst != null){
				usedCandidates.add(candInst);
				confusion.TPP ++;
				confusion.TP += getWeightedScore(goldInst,candInst);
			}else{
				confusion.FN ++;
				errors.get(ConfusionLabel.FN).add(goldInst);
			}
			
		}
		for(IInstance inst: candidateVariables){
			if(!usedCandidates.contains(inst)){
				confusion.FP ++;
				errors.get(ConfusionLabel.FP).add(inst);
			}
		}
	}
	
	
	private IInstance getMatchingAnnotationVaiable(List<IInstance> candidateVariables, IInstance goldInst) {
		IProperty prop = null; 
		String goldSpan = ""+goldInst.getPropertyValue(goldInst.getOntology().getProperty(DomainOntology.HAS_SPAN));
		IClass goldType = goldInst.getDirectTypes()[0];
		IInstance candidateAnnotation = null;
		int candidateSpan = 0;
		for(IInstance inst: candidateVariables){
			if(prop == null)
				prop = inst.getOntology().getProperty(DomainOntology.HAS_SPAN);
			String span = ""+inst.getPropertyValue(prop);
			IClass type = inst.getDirectTypes()[0];
			// if candidate type is identical to gold or more specific
			if(type.equals(goldType) || type.hasSuperClass(goldType)){
				int overlap = spanOverlap(goldSpan,span);
				if(overlap > candidateSpan){
					candidateAnnotation = inst;
					candidateSpan = overlap;
				}
			}
			
		}
		return candidateAnnotation;
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

	private class Span {
		int start,end;
		public boolean overlaps(Span s){
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
			String [] p = span.split(SPAN_SEPERATOR);
			if(p.length == 2){
				Span sp = new Span();
				sp.start = Integer.parseInt(p[0]);
				sp.end = Integer.parseInt(p[1]);
				list.add(sp);
			}
		}
		return list;
	}

	private double getWeightedScore(IInstance goldInst, IInstance candInst) {
		double score = 0;
		// equal weights for now
		int total =  0;
		for(IProperty prop: goldInst.getProperties()){
			if(prop.isObjectProperty()){
				total ++;
				score += compareValues(goldInst,candInst,prop);
			}
		}
		return score/ total;
	}


	private double compareValues(IInstance goldInst, IInstance candInst, IProperty goldProp) {
		//double weight = 1.0;
		double score = 0.0;
		int total = 0;
		IProperty prop = candInst.getOntology().getProperty(goldProp.getName());
		if(goldProp.isObjectProperty()){
			for(IInstance gVal: getInstanceValues(goldInst.getPropertyValues(goldProp))){
				total ++;
				for(IInstance cVal: getInstanceValues(candInst.getPropertyValues(prop))){
					// assume single direct type
					// if class names of both instnaces matched, we got a winnder
					if(gVal.getDirectTypes()[0].equals(cVal.getDirectTypes()[0])){
						score ++;
						break; // don't double count statters
					}
				}
			}
		}
		return (total > 0)?score/total:0;
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
