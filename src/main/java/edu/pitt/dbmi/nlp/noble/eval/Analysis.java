package edu.pitt.dbmi.nlp.noble.eval;

import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tseytlin on 3/25/17.
 * This class encapsulate analys
 */
public class Analysis {
    public static int MAX_ATTRIBUTE_SIZE = 10;
    public static enum ConfusionLabel {
        TP,FP,FN,TN
    }
    public static class ConfusionMatrix {
        public double TPP,TP,FP,FN,TN;
        public void append(AnnotationEvaluation.ConfusionMatrix c){
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

    private Map<String,AnnotationEvaluation.ConfusionMatrix> confusions;
    private Map<String,Map<String,List<IInstance>>> errorMap;

    /**
     * get generated confusion matricies
     * @return map of confusion matricies
     */
    public Map<String,AnnotationEvaluation.ConfusionMatrix> getConfusionMatricies(){
        if(confusions == null)
            confusions = new LinkedHashMap<String, AnnotationEvaluation.ConfusionMatrix>();
        return confusions;
    }

    /**
     * get confusion matrix for a given label, create one if not there
     * @param name - name of the confusion matrix
     * @return confusion matrix
     */
    public AnnotationEvaluation.ConfusionMatrix getConfusionMatrix(String name) {
        AnnotationEvaluation.ConfusionMatrix confusion = getConfusionMatricies().get(name);
        if(confusion == null){
            confusion = new AnnotationEvaluation.ConfusionMatrix();
            getConfusionMatricies().put(name,confusion);
        }
        return confusion;
    }

    /**
     * get error map for a given analysis
     * @return map of labels to document to instances
     */
    public Map<String,Map<String,List<IInstance>>> getErrorMap(){
        if(errorMap == null)
            errorMap = new LinkedHashMap<String, Map<String, List<IInstance>>>();
        return  errorMap;
    }

    /**
     * get error map for a given label
     * @param label - the label
     * @return map of files to instance lists
     */
    public Map<String,List<IInstance>> getErrorMap(String label){
        Map<String,List<IInstance>> errors = getErrorMap().get(label);
        if(errors == null) {
            errors = new LinkedHashMap<String, List<IInstance>>();
            getErrorMap().put(label,errors);
        }
        return errors;
    }

    /**
     * add error to an analysis
     * @param label - label s.a. Mention or Class name or Mention.TP
     * @param report - report where error has happend
     * @param inst - instance of an error
     */
    public void addError(String label, String report, IInstance inst){
        List<IInstance> list = getErrorMap(label).get(report);
        if(list == null){
            list = new ArrayList<IInstance>();
            getErrorMap(label).put(report,list);
        }
        list.add(inst);
    }
}
