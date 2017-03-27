package edu.pitt.dbmi.nlp.noble.eval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

/**
 * print attribute weights for SemEval weighted measure
 * @author tseytlin
 *
 */
public class AttributeWeights {

	public static void main(String[] args) throws Exception {
		// compare two files
		if(args.length >= 1){
			File gold = new File(args[0]);
			AttributeWeights pe = new AttributeWeights();
			pe.printWeights(pe.computeWeights(gold),System.out);	
		}else{
			System.err.println("Usage: java "+AttributeWeights.class.getSimpleName()+" <gold .owl file>");
		}

	}

	/**
	 * print weights matrix
	 * @param computeWeights - weight matrix
	 * @param out - print stream
	 */
	private void printWeights(Map<String,Double> computeWeights, PrintStream out) {
		for(String key: computeWeights.keySet()){
			out.println(key+"\t"+TextTools.toString(computeWeights.get(key)));
		}
	}
	
	/**
	 * get weights as string
	 * @param computeWeights - weight matrix
	 * @return String representation of them
	 */
	public String getWeightsAsText(Map<String,Double> computeWeights){
		StringBuilder str = new StringBuilder();
		for(String key: computeWeights.keySet()){
			str.append(key+"\t"+TextTools.toString(computeWeights.get(key))+"\n");
		}
		return str.toString();
	}
	
	/**
	 * write weights to a file
	 * @param computeWeights - matrix of weights
	 * @param outFile - output file
	 * @throws IOException exception to be thrown
	 */
	public void writeWeights(Map<String,Double> computeWeights,File outFile) throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
		writer.write(getWeightsAsText(computeWeights));
		writer.close();
	}
	

	/**
	 * compute weights matrix
	 * @param gold
	 * @return
	 * @throws IOntologyException
	 */
	public Map<String,Double> computeWeights(File gold) throws IOntologyException {
		Map<String,Double> weights = new LinkedHashMap<String, Double>();
		Map<String,Map<String,Double>> counts = new LinkedHashMap<String, Map<String,Double>>();
		IOntology ont = OOntology.loadOntology(gold);
		for(IClass cls: ont.getClass("Annotation").getSubClasses()){
			for(IInstance inst: cls.getDirectInstances()){
				for(IProperty prop : inst.getProperties()){
					if(prop.isObjectProperty()){
						countModifier(inst,prop,counts);
					}
				}
			}
		}
		// now compute weights
		for(String prop: counts.keySet()){
			Map<String,Double> map = counts.get(prop);
			double total = 0;
			// compute total
			for(String modifier: map.keySet()){
				total +=  map.get(modifier);
			}
			// compute weights
			for(String modifier: map.keySet()){
				double count =  map.get(modifier);
				double weight = 1-(count/total);
				weights.put(modifier,weight);
			}
		}
		
		return weights;
		
	}

	/**
	 * count modifiers for a given annotation instance on a given property
	 * @param inst - annotation instance
	 * @param prop - modifier property
	 * @param counts - counts map
	 */
	private void countModifier(IInstance inst, IProperty prop, Map<String, Map<String, Double>> counts) {
		Map<String,Double> map = counts.get(prop.getName());
		if(map == null){
			map = new HashMap<String, Double>();
			counts.put(prop.getName(),map);
		}
		for(Object obj: inst.getPropertyValues(prop)){
			if(obj instanceof IInstance){
				IClass modifierCls = ((IInstance) obj).getDirectTypes()[0];
				Double num = map.get(modifierCls.getName());
				if(num == null)
					num = new Double(0);
				map.put(modifierCls.getName(),num.doubleValue()+1);
			}
		}
	}

}
