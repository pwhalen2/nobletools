package edu.pitt.dbmi.nlp.noble.eval;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

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
import edu.pitt.dbmi.nlp.noble.util.FileTools;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;
import edu.pitt.dbmi.nlp.noble.util.UITools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class AnnotationEvaluation implements ActionListener {
	public static final String DISJOINT_SPANS = "\\s+"; // span seperate
	public static final String SPAN_SEPERATOR = ":";    //within a span Ex: 12:45
	public static final String ANALYSIS_HTML = "analysis.html";
	public static final String ANALYSIS_TSV = "analysis.tsv";
	public static boolean STRICT_VALUE_CALCULATION = false;
	public static boolean PRINT_RECORD_LEVEL_STATS = false;
	private Map<String,Double> attributeWeights;
	private Set<IClass> validAnnotations;
	private Analysis analysis;
	
	// UI components
	private JDialog dialog;
	private JTextField goldOntology, systemOntology, goldWeights;
	private JTextArea console;
	private JPanel buttonPanel;
	private JProgressBar progress;
	private File lastFile;
	
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
			pe.getDialog().setVisible(true);
			/*
			pe.loadWeights(weights);
			pe.evaluate(gold,candidate);
			pe.outputHTML(candidate.getParentFile());
			*/
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
	 * output TSV to file
	 * @param parentFile
	 * @throws FileNotFoundException
	 */
	public void outputTSV(File parentFile) throws FileNotFoundException {
		PrintStream fos = new PrintStream(new File(parentFile,ANALYSIS_TSV));
		getAnalysis().printResultTable(fos);
		fos.close();
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
	
	public void evaluate(File file1, File file2) throws IOException, IOntologyException {
		IOntology goldInstances = OOntology.loadOntology(file1);
		IOntology systemInstances = OOntology.loadOntology(file2);
	
		// load valid annotations for totals
		loadAnnotationFilter(goldInstances);
		
		// init confusionMatrix
		analysis = new Analysis();
		analysis.setTitle("Results for "+file1.getName()+" on "+new Date());
		Analysis.ConfusionMatrix mentionConfusion = analysis.getConfusionMatrix(" Overall Mention");
		//Analysis.ConfusionMatrix documentConfusion = analysis.getConfusionMatrix(" Document");

		
		// get composition
		List<IInstance> goldCompositions = getCompositions(goldInstances);
		List<IInstance> candidateCompositions = getCompositions(systemInstances);
		

		for(IInstance gold: goldCompositions){
			IInstance cand = getMatchingComposition(candidateCompositions,gold);
			if(cand != null){
				calculateDocumentConfusion(gold, cand,DomainOntology.HAS_MENTION_ANNOTATION,mentionConfusion);
				//calculateDocumentConfusion(gold, cand,DomainOntology.HAS_DOCUMENT_ANNOTATION,documentConfusion);
			}
		}
		
		// print results
		analysis.printResultTable(System.out);
	}

	/**
	 * go over 
	 * @param ontology
	 */
	private void loadAnnotationFilter(IOntology ontology) {
		validAnnotations = new HashSet<IClass>();
		for(IClass cls: ontology.getClass(DomainOntology.ANNOTATION).getSubClasses()){
			if(cls.getDirectInstances().length > 0)
				validAnnotations.add(cls);	
		}
	}

	private Set<IClass> getAnnotationFilter(){
		if(validAnnotations == null)
			validAnnotations = new HashSet<IClass>();
		return validAnnotations;
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
				// there could be some annotations that we simply don't evaluate because
				// GOLD didn't bother to annotate them
				if(getAnnotationFilter().contains(inst.getDirectTypes()[0])){
					confusion.FP ++;
					getAnalysis().addError(confusion.getLabelFP(),docTitle,inst);
					getConfusionMatrix(inst).FP++;
					getAnalysis().addError(getConfusionMatrix(inst).getLabelFP(),docTitle,inst);
				}
				
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
		IClass goldType = null;
		
		// set to the percent of overlap of gold 
		double overlapThreshold = 0;
		
		// go through all possible candidate variables and select the ones that are the same type (or more specific)
		// and have a span overlap above threshold
		for(IInstance inst: candidateVariables){
			if(prop == null)
				prop = inst.getOntology().getProperty(DomainOntology.HAS_SPAN);
			if(goldType == null)
				goldType = inst.getOntology().getClass(goldInst.getDirectTypes()[0].getName());
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
		//double defaultWeight =  1.0; //getDefaultWeight(goldInst);
		double numerator   = 1.0;  // initial weight of an anchor
		double denominator = 1.0;  // initial total score 
		for(IProperty goldProp: getProperties(goldInst)){
			for(IInstance gVal: getInstanceValues(goldInst.getPropertyValues(goldProp))){
				double weight = getWeight(gVal);
				//if(weight == 0)
				//	weight = defaultWeight;
				denominator += weight;
				numerator += weight * hasAttributeValue(systemInst,goldProp,gVal);
			}
		}
		return numerator / denominator;
	}

	/*private double getDefaultWeight(IInstance inst){
		double count = 0;
		for(IProperty goldProp: getProperties(inst)) {
			for (IInstance gVal : getInstanceValues(inst.getPropertyValues(goldProp))) {
				count++;
			}
		}
		return count > 0?1.0 / count:0;
	}*/

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
		//System.err.println("no weight for: "+name);
		return 1.0;
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
	
	
	public JDialog getDialog(){
		return getDialog(null);
	}
	
	public JDialog getDialog(Frame owner){
		if(dialog == null){
			dialog = new JDialog(owner,"Annotation Evaluation",false);
			//dialog.setIconImage(new ImageIcon(LOGO_ICON).getImage());
			
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0);
			GridBagLayout l = new GridBagLayout();
			l.setConstraints(panel,c);
			panel.setLayout(l);
			
			// gold ontology instances
			goldOntology = new JTextField(30);
			JButton browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("g_browser");
			
			panel.add(new JLabel("Gold Instantiated Ontology"),c);c.gridx++;
			panel.add(goldOntology,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
	
			goldWeights = new JTextField(30);
			browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("w_browser");
			
			panel.add(new JLabel("Gold Weights File"),c);c.gridx++;
			panel.add(goldWeights,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
			
			
			systemOntology = new JTextField(30);
			browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("s_browser");
		
			panel.add(new JLabel("System Instantiated Ontology "),c);c.gridx++;
			panel.add(systemOntology,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
			panel.add(Box.createRigidArea(new Dimension(10,10)),c);
			
			JPanel conp = new JPanel();
			conp.setLayout(new BorderLayout());
			conp.setBorder(new TitledBorder("Output Results"));
			console = new JTextArea(10,40);
			//console.setLineWrap(true);
			console.setEditable(false);
			conp.add(new JScrollPane(console),BorderLayout.CENTER);
			//c.gridwidth=3;		
			//panel.add(conp,c);c.gridy++;c.gridx=0;
			
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new GridLayout(1,2,10,10));
			buttonPanel.setBorder(new EmptyBorder(10,30,10,30));
			
			JButton generate = new JButton("Generate Weights");
			generate.addActionListener(this);
			generate.setActionCommand("weights");
			
			JButton run = new JButton("Evaluate");
			run.addActionListener(this);
			run.setActionCommand("evaluate");
			buttonPanel.add(generate);
			buttonPanel.add(run);
			//panel.add(buttonPanel,c);
			
			progress = new JProgressBar();
			progress.setIndeterminate(true);
			progress.setString("Please Wait. It may take a while ...");
			progress.setStringPainted(true);
			
			JPanel p = new JPanel();
			p.setLayout(new BorderLayout());
			p.add(panel,BorderLayout.NORTH);
			p.add(conp,BorderLayout.CENTER);
			p.add(buttonPanel,BorderLayout.SOUTH);
			
				
			// wrap up, and display
			dialog.setContentPane(p);
			dialog.pack();
		
			//center on screen
			Dimension d = dialog.getSize();
			Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
			dialog.setLocation(new Point((s.width-d.width)/2,(s.height-d.height)/2));
			
		}
		return dialog;	
	}
	
	/**
	 * set busy .
	 *
	 * @param b the new busy
	 */
	private void setBusy(boolean b){
		final boolean busy = b;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(busy){
					progress.setIndeterminate(true);
					progress.setString("Please Wait. It may take a while ...");
					progress.setStringPainted(true);
					getDialog().getContentPane().remove(buttonPanel);
					getDialog().getContentPane().add(progress,BorderLayout.SOUTH);
					console.setText("");
				}else{
					getDialog().getContentPane().remove(progress);
					getDialog().getContentPane().add(buttonPanel,BorderLayout.SOUTH);
				}
				getDialog().getContentPane().revalidate();
				getDialog().pack();
				
			}
		});
	}

	public void setSystemInstanceOntlogy(String text){
		systemOntology.setText(text);
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("evaluate".equals(cmd)){
			doEvaluate();
		}else if("g_browser".equals(cmd)){
			doBrowse(goldOntology);
		}else if("w_browser".equals(cmd)){
			doBrowse(goldWeights);
		}else if("s_browser".equals(cmd)){
			doBrowse(systemOntology);
		}else if("exit".equals(cmd)){
			System.exit(0);
		}else if("weights".equals(cmd)){
			doWeights();
		}	
	}
	
	private void doWeights() {
		new Thread(new Runnable() {
			public void run() {
				File gold = new File(goldOntology.getText());
				File weights = new File(goldWeights.getText());
				if(!gold.exists()){
					JOptionPane.showMessageDialog(getDialog(),"Can't find gold instance ontology: "+gold,"Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				if(!weights.getParentFile().canWrite()){
					JOptionPane.showMessageDialog(getDialog(),"Can't save gold weights file to "+weights.getParentFile(),"Error",JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				setBusy(true);
				try{
				
					AttributeWeights pe = new AttributeWeights();
					Map<String,Double> weightMap = pe.computeWeights(gold);
					pe.writeWeights(weightMap, weights);
					final String text = pe.getWeightsAsText(weightMap);
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							console.setText(text);
							console.repaint();
						}
					});
					
				}catch(Exception ex){
					JOptionPane.showMessageDialog(getDialog(),"There was a prolbem with evaluation: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
					return;
				}finally {
					setBusy(false);
				}
			}
		}).start();
		
	}

	private void doEvaluate() {
		new Thread(new Runnable() {
			public void run() {
				File gold = new File(goldOntology.getText());
				File weights = new File(goldWeights.getText());
				File candidate = new File(systemOntology.getText());
				if(!gold.exists()){
					UITools.showErrorDialog(getDialog(),"Can't find gold instance ontology: "+gold);
					return;
				}
				if(!candidate.exists()){
					UITools.showErrorDialog(getDialog(),"Can't find system instance ontology: "+candidate);
					return;
				}
				if(goldWeights.getText().length() > 0 && !weights.exists()){
					UITools.showErrorDialog(getDialog(),"Can't find gold weights file: "+weights);
					return;
				}
				
				setBusy(true);
				try{
					if(weights.exists())
						loadWeights(weights);
					evaluate(gold,candidate);
					outputHTML(candidate.getParentFile());
					outputTSV(candidate.getParentFile());
					
					// output result
					final String text = getAnalysis().getResultTableAsText();
					SwingUtilities.invokeLater(new Runnable(){
						public void run(){
							console.setText(text);
							console.repaint();
						}
					});
					
					// open in browser
					try{
						UITools.browseURLInSystemBrowser(new File(candidate.getParentFile().getAbsolutePath()+File.separator+ANALYSIS_HTML).toURI().toString());
					}catch(Exception ex){
						UITools.showErrorDialog(getDialog(),ex);
					}
					
				}catch(Exception ex){
					UITools.showErrorDialog(getDialog(),"There was a prolbem with evaluation: ",ex);
					return;
				}finally {
					setBusy(false);
				}
			}

		
		}).start();
	}

	/**
	 * Do browse.
	 *
	 * @param text the text
	 */
	private void doBrowse(JTextField text){
		File file = text.getText().length() > 0? new File(text.getText()):lastFile;
		JFileChooser fc = new JFileChooser(file);
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.addChoosableFileFilter(new FileFilter() {
			public String getDescription() {
				return "Text files (.txt)";
			}
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".txt");
			}
		});
	
		int r = fc.showOpenDialog(getDialog());
		if(r == JFileChooser.APPROVE_OPTION){
			file = fc.getSelectedFile();
			text.setText(file.getAbsolutePath());
			lastFile = file;
		}
	}

}
